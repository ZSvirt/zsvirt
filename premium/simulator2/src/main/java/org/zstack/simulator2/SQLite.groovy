package org.zstack.simulator2

import org.sqlite.SQLiteConfig
import org.zstack.core.Platform
import org.zstack.simulator2.config.Col
import org.zstack.simulator2.config.VO
import org.zstack.utils.FieldUtils
import org.zstack.utils.TypeUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.sql.DriverManager
import java.sql.Statement

/**
 * Created by xing5 on 2017/9/18.
 */
class SQLite {
    CLogger logger = Utils.getLogger(SQLite.class)

    def config = new SQLiteConfig()

    private String dbPath

    class FK {
        Class clz
        String fieldName
    }

    Map<Class, List<FK>> foreignKeys = [:]
    Map<Class, List<Field>> voFields = [:]
    Collection<Class> voClasses

    SQLite(String dbPath) {
        this.dbPath = dbPath

        Class.forName("org.sqlite.JDBC")
        config.enforceForeignKeys(true)

        voClasses = Platform.reflections.getSubTypesOf(VO.class).findAll { !Modifier.isAbstract(it.modifiers) }
        voClasses.each { clz ->
            voFields[clz] = FieldUtils.getAllFields(clz).findAll { it.isAnnotationPresent(Col.class) }
        }

        voFields.each { clz, fields ->
            fields.each { f ->
                Col at = f.getAnnotation(Col.class)
                Class parent = at.parent()
                if (parent == Object.class) {
                    return
                }

                Collection<Class> parents = Platform.reflections.getSubTypesOf(parent).findAll { !Modifier.isAbstract(it.modifiers) }
                parents.each { pclz ->
                    List<FK> existingFKs = foreignKeys[pclz]
                    if (existingFKs == null) {
                        existingFKs = []
                        foreignKeys[pclz] = existingFKs
                    }

                    FK fk = new FK()
                    fk.clz = clz
                    fk.fieldName = f.name
                    existingFKs.add(fk)
                }
            }
        }
    }

    void createTables() {
        voClasses.each { voClz ->
            def fields = FieldUtils.getAllFields(voClz).findAll { !Modifier.isTransient(it.modifiers) && !Modifier.isStatic(it.modifiers) }

            List fieldTxts = fields.findAll { it.name != "id" }.collect {
                Col at = it.getAnnotation(Col.class)
                boolean notNull = at == null ? false : at.notNull()
                List txt = [it.name]
                if (String.class.isAssignableFrom(it.type)) {
                    txt.add("TEXT")
                } else if (TypeUtils.isTypeOf(it.type, Long.class, Short.class, Integer.class,int.class, short.class, long.class)) {
                    txt.add("INTEGER")
                } else if (TypeUtils.isTypeOf(Boolean.class, boolean.class)) {
                    txt.add("BOOLEAN")
                } else {
                    assert false : "${voClz}.${it.name} has a unsupported type[${it.type}]"
                }

                if (notNull) {
                    txt.add("NOT NULL,")
                } else {
                    txt.add(",")
                }

                return txt.join(" ")
            }

            fieldTxts.add("id TEXT PRIMARY KEY")

            def sql = """\n
CREATE TABLE ${voClz.simpleName} (
${fieldTxts.join("\n")}
)"""

            execute(sql)
        }
    }

    private  <T> T exec(Closure c) {
        def conn = DriverManager.getConnection("jdbc:sqlite:$dbPath", config.toProperties())
        conn.autoCommit = false
        try {
            def stmt = conn.createStatement()
            def ret = c(stmt)
            stmt.close()
            conn.commit()
            return ret
        } finally {
            conn.close()
        }
    }


    def <T> T find(String sql, Class clz) {
        List ret = query(sql, clz)
        assert ret.size() < 2 : "multiple results found for '${sql}'"

        return ret.size() == 1 ? ret[0] : null
    }

    boolean existById(String id, Class clz){
        def rets = query("select count(*) from ${clz.simpleName} where id = '${id}'",clz)
        return rets[0] != 0
    }

    List query(String sql, Class clz) {
        assert sql != null

        sql = sql.toString()
        List rows = []

        exec { Statement stmt ->
            logger.debug(sql)
            def ret = stmt.executeQuery(sql)
            try {
                def md = ret.metaData
                def cols = md.columnCount

                while (ret.next()) {
                    def row = [:]
                    for (int i=1; i<=cols; i++) {
                        row[md.getColumnName(i)] = ret.getObject(i)
                    }

                    rows.add(row)
                }
            } finally {
                ret.close()
            }
        }

        List objs = []
        rows.each { row ->
            def obj = clz.newInstance()
            row.each { k, v ->
                obj.putAt(k, v)
            }

            objs.add(obj)
        }

        return objs
    }

    private void executeStatement(String sql, Statement stmt) {
        logger.debug(sql)
        stmt.execute(sql)
    }

    void execute(List<String> sqls) {
        exec { Statement stmt ->
            sqls.each { sql ->
                executeStatement(sql, stmt)
            }
        }
    }

    void execute(String sql) {
        exec { Statement stmt -> executeStatement(sql, stmt) }
    }

    def <T> T findById(String id, Class clz) {
        def rets = query("select * from ${clz.simpleName} where id = '${id}'", clz)
        assert rets.size() < 2 : "duplicated records for ${clz.simpleName} with id[${id}]"
        return rets.isEmpty() ? null : rets[0] as T
    }

    void updateById(String id, Class clz, Map<String, Object> values) {
        List<String> pairs = values.collect { k, v->
            if (v instanceof String) {
                return "${k}='${v}'"
            } else {
                return "${k}=${v}"
            }
        }

        String sql = "update ${clz.simpleName} set ${pairs.join(",")} where id = '${id}'"
        execute(sql)
    }

    void delteById(String id, Class clz) {
        List<String> sqls = ["delete from ${clz.simpleName} where id = '${id}'"]

        List<FK> fks = foreignKeys[clz]
        if (fks != null) {
            sqls.addAll(fks.collect {
                return "delete from ${it.clz.simpleName} where ${it.fieldName} = '${id}'"
            })
        }

        execute(sqls)
    }

    void persist(Object obj) {
        assert obj != null : "obj cannot be null"

        List names = []
        List value = []

        List<Field> fields = voFields[obj.class]
        assert fields != null : "${obj.class} is not a VO class"

        fields.each {
            names.add(it.name)
            def v = obj.getAt(it.name)
            if (v == null) {
                value.add(null)
                return
            }

            if (String.class.isAssignableFrom(it.type)) {
                value.add("'" + v + "'")
            } else {
                value.add(v)
            }
        }

        def sql = "insert into ${obj.class.simpleName} (${names.join(",")}) values (${value.join(",")})"
        execute(sql)
    }
}

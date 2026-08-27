package org.zstack.testlib.dbremaining

abstract class AllowedDBRemaining {
    List<Table> tables = []

    private boolean resolved

    void resolve() {
        if (!resolved) {
            remaining()
            resolved = true
        }
    }

    abstract void remaining()

    class Table {
        Class tableVOClass
        boolean noLimitRows
        Closure<List> checker

        List check(List vos) {
            if (noLimitRows) {
                return []
            }

            if (checker) {
                vos = checker(vos)
                assert vos != null : "checker must return a list of remained VOs"
            }

            return vos
        }
    }

    void table(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Table.class) Closure c) {
        def t = new Table()
        c.delegate = t
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        tables.add(t)
    }

    List check(String tblName, List vos) {
        resolve()

        List<Table> ts = tables.findAll { it.tableVOClass.simpleName == tblName }
        ts.each {
            vos = it.check(vos)
        }

        return vos
    }
}

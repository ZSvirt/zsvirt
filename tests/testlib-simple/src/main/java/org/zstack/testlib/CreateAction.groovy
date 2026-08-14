package org.zstack.testlib
/**
 * Created by xing5 on 2017/2/12.
 */
trait CreateAction {
    // return uuid of the created resource
    abstract SpecID create(String uuid, String sessionId)

    boolean onlyDefine = false

    SpecID id(String name, String uuid) {
        return new SpecID(name, uuid)
    }

    List<Closure> preOperations = []
    List<Closure> postOperations = []
    List<Closure> beforeOperations = []
    List<Closure> afterOperations = []

    void beforeCreate(Closure cl) {
        beforeOperations.add(cl)
    }

    void afterCreate(Closure cl) {
        afterOperations.add(cl)
    }

    void preCreate(Closure cl) {
        preOperations.add(cl)
    }

    void postCreate(Closure cl) {
        postOperations.add(cl)
    }
}
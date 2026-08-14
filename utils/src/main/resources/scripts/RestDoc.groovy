package scripts

/**
 * Created by xing5 on 2016/12/23.
 */
class RestDoc {
    private String titleValue

    private static final List<RestDoc> docs = []

    static void make(Closure closure) {
        RestDoc doc = new RestDoc()
        docs.add(doc)

        closure.delegate = doc
        closure()
    }

    def title(String txt) {
        titleValue = txt
    }
}

package org.zstack.testlib.http

import org.zstack.header.errorcode.ErrorableValue

import java.util.function.BiFunction
import java.util.function.Predicate

class PostHandlerPair<PARAM, RAW_RESULT> {
    Predicate<PARAM> condition
    BiFunction<PARAM, ErrorableValue<RAW_RESULT>, ErrorableValue<RAW_RESULT>> runIfMatch

    PostHandlerPair(Predicate<PARAM> condition, BiFunction<PARAM, ErrorableValue<RAW_RESULT>, ErrorableValue<RAW_RESULT>> runIfMatch) {
        this.condition = condition
        this.runIfMatch = runIfMatch
    }
}

package org.zstack.tag;

import org.zstack.core.db.Q;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.TagAO_;
import org.zstack.header.tag.UserTagVO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by lining on 2017/6/22.
 */
public class SystemTagUtils {

    public static String findTagValue(List<String> systemTags, SystemTag tag, String tagToken){
        if(systemTags == null || tag == null || tagToken == null ){
            throw new IllegalArgumentException("illegal argument");
        }

        if(systemTags.isEmpty()){
            return null;
        }

        Optional<String> opt = systemTags.stream().filter(tag::isMatch).findAny();
        return opt.map(s -> tag.getTokenByTag(s, tagToken)).orElse(null);
    }

    public static String findTagValue(List<String> systemTags, SystemTag tag){
        if(systemTags == null || tag == null){
            throw new IllegalArgumentException("illegal argument");
        }

        if(systemTags.isEmpty()){
            return null;
        }

        Optional<String> opt = systemTags.stream().filter(tag::isMatch).findAny();
        return opt.orElse(null);
    }

    public static List<String> findTagValues(List<String> systemTags, SystemTag tag, String tagToken) {
        if(systemTags == null || tag == null || tagToken == null ){
            throw new IllegalArgumentException("illegal argument");
        }

        if(systemTags.isEmpty()){
            return null;
        }

        return systemTags.stream().filter(tag::isMatch).map(t -> tag.getTokenByTag(t, tagToken)).collect(Collectors.toList());
    }

    public static List<String> findTagValues(List<String> systemTags, SystemTag tag) {
        if(systemTags == null || tag == null){
            throw new IllegalArgumentException("illegal argument");
        }

        if(systemTags.isEmpty()){
            return null;
        }

        return systemTags.stream().filter(tag::isMatch).collect(Collectors.toList());
    }

    public static String findSystemTagOwner(String tagUuid) {
        return findTagOwner(tagUuid, SystemTagVO.class);
    }

    public static String findUserTagOwner(String tagUuid) {
        return findTagOwner(tagUuid, UserTagVO.class);
    }

    public static String findTagOwner(String tagUuid, Class<?> tagClass) {
        return Q.New(tagClass, AccountResourceRefVO.class)
                .table0()
                    .eq(TagAO_.uuid, tagUuid)
                    .eq(TagAO_.resourceUuid).table1(AccountResourceRefVO_.resourceUuid)
                .table1()
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .select(AccountResourceRefVO_.accountUuid)
                .find();
    }
}

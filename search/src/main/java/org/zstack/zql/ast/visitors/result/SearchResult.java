package org.zstack.zql.ast.visitors.result;

import org.hibernate.search.jpa.FullTextQuery;
import org.zstack.core.db.DBGraph;

import java.util.List;
import java.util.Map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 16:40 2020/10/27
 */
public class SearchResult {

    public static class Search {
        FullTextQuery query;
        String restrictSql;

        public FullTextQuery getQuery() {
            return query;
        }

        public void setQuery(FullTextQuery query) {
            this.query = query;
        }

        public String getRestrictSql() {
            return restrictSql;
        }

        public void setRestrictSql(String restrictSql) {
            this.restrictSql = restrictSql;
        }
    }

    public List<Search> searchs;

    public List<Search> getSearchs() {
        return searchs;
    }

    public void setSearchs(List<Search> searchs) {
        this.searchs = searchs;
    }
}

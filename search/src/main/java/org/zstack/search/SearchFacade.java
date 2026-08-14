package org.zstack.search;

import org.hibernate.search.jpa.FullTextEntityManager;

public interface SearchFacade {

    FullTextEntityManager getFullTextEntityManager();
}

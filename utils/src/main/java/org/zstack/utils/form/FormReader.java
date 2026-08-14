package org.zstack.utils.form;

public interface FormReader {
    String getType();
    String[] getHeader();

    /**
     * get next record, exclude header.
     * @return
     *      NULL when form has no more records;
     *      new String[0] if next record has no data.
     */
    String[] nextRecord();
}

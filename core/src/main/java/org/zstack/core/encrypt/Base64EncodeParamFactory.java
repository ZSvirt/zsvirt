package org.zstack.core.encrypt;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;

public class Base64EncodeParamFactory implements TypeAdapterFactory {
    private static TypeAdapter adapter = Base64TypeAdapter.create();

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        return adapter;
    }

    private static final class Base64TypeAdapter<T>
            extends TypeAdapter<T> {

        static  <T> TypeAdapter<T> create() {
            return new Base64TypeAdapter<T>()
                    .nullSafe(); // Just let Gson manage nulls itself. It's convenient
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            String output = value.toString();

            if (Base64.isBase64(output)) {
                out.value(output);
                return;
            }

            out.value(new String(Base64.encodeBase64(output.getBytes())));
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            String input = in.nextString();

            if (!Base64.isBase64(input)) {
                return (T) input;
            }

            return (T) new String(Base64.decodeBase64(input));
        }
    }
}

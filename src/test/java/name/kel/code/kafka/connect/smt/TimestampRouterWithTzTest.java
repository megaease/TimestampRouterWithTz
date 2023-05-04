/*
 MIT License

Copyright (c) 2023 Kel Graham

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package name.kel.code.kafka.connect.smt;

import org.apache.kafka.connect.source.SourceRecord;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import static org.junit.Assert.*;

public class TimestampRouterWithTzTest {
    private final TimestampRouterWithTz<SourceRecord> xform = new TimestampRouterWithTz<>();

    @After
    public void teardown() {
        xform.close();
    }

    @Test
    public void defaultConfiguration() {
        xform.configure(Collections.emptyMap()); // defaults
        final SourceRecord record = new SourceRecord(
                null, null,
                "test", 0,
                null, null,
                null, null,
                1483425001864L
        );
        assertEquals("test-20170103", xform.apply(record).topic());
    }

    @Test
    public void tzConversionWithUTZ() {

        // Explictly set UTC timezone (the default)
        HashMap<String, String> conf = new HashMap<>();
        conf.put("tz", "UTC");


        xform.configure(conf);
        final SourceRecord record = new SourceRecord(
                null, null,
                "test", 0,
                null, null,
                null, null,
                1682978400000L
        );
        assertEquals("test-20230501", xform.apply(record).topic());
    }

    @Test
    public void tzConversionWithNZ() {
        
        // Set TZ to New Zealand
        HashMap<String, String> conf = new HashMap<>();
        conf.put("tz", "Pacific/Auckland");

        xform.configure(conf);
        final SourceRecord record = new SourceRecord(
                null, null,
                "test", 0,
                null, null,
                null, null,
                1682978400000L
        );

        // Our message timestamp is Monday, 1 May 2023 22:00:00 GMT
        // Which when converted to NZ tz is 2nd of May
        assertEquals("test-20230502", xform.apply(record).topic());
    }

}
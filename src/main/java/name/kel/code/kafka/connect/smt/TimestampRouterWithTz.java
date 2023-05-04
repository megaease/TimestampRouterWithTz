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

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import org.apache.kafka.connect.transforms.Transformation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimestampRouterWithTz<R extends ConnectRecord<R>> implements Transformation<R>, AutoCloseable {

    private static final Pattern TOPIC = Pattern.compile("${topic}", Pattern.LITERAL);

    private static final Pattern TIMESTAMP = Pattern.compile("${timestamp}", Pattern.LITERAL);

    private static String tz = "UTC";

    public static final String OVERVIEW_DOC =
            "Update the record's topic field as a function of the original topic value and the record timestamp."
                    + "<p/>"
                    + "This is mainly useful for sink connectors, since the topic field is often used to determine the equivalent entity name in the destination system"
                    + "(e.g. database table or search index name).";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(ConfigName.TOPIC_FORMAT, ConfigDef.Type.STRING, "${topic}-${timestamp}", ConfigDef.Importance.HIGH,
                    "Format string which can contain <code>${topic}</code> and <code>${timestamp}</code> as placeholders for the topic and timestamp, respectively.")
            .define(ConfigName.TIMESTAMP_FORMAT, ConfigDef.Type.STRING, "yyyyMMdd", ConfigDef.Importance.HIGH,
                    "Format string for the timestamp that is compatible with <code>java.text.SimpleDateFormat</code>.")
            .define(ConfigName.TZ, ConfigDef.Type.STRING, "UTC", ConfigDef.Importance.LOW,
                    "Timezone to apply to timestamp prior to formatting. Default is <code>UTC</code>.");
                    ;

    private interface ConfigName {
        String TOPIC_FORMAT = "topic.format";
        String TIMESTAMP_FORMAT = "timestamp.format";
        String TZ = "tz";
    }

    private String topicFormat;
    private ThreadLocal<SimpleDateFormat> timestampFormat;

    @Override
    public void configure(Map<String, ?> props) {
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, props);

        topicFormat = config.getString(ConfigName.TOPIC_FORMAT);

        final String timestampFormatStr = config.getString(ConfigName.TIMESTAMP_FORMAT);
        timestampFormat = ThreadLocal.withInitial(() -> {
            final SimpleDateFormat fmt = new SimpleDateFormat(timestampFormatStr);
            fmt.setTimeZone(TimeZone.getTimeZone(config.getString(ConfigName.TZ)));
            
            return fmt;
        });
    }

    @Override
    public R apply(R record) {
        final Long timestamp = record.timestamp();
        if (timestamp == null) {
            throw new DataException("Timestamp missing on record: " + record);
        }
        final String formattedTimestamp = timestampFormat.get().format(new Date(timestamp));

        final String replace1 = TOPIC.matcher(topicFormat).replaceAll(Matcher.quoteReplacement(record.topic()));
        final String updatedTopic = TIMESTAMP.matcher(replace1).replaceAll(Matcher.quoteReplacement(formattedTimestamp));
        return record.newRecord(
                updatedTopic, record.kafkaPartition(),
                record.keySchema(), record.key(),
                record.valueSchema(), record.value(),
                record.timestamp()
        );
    }

    @Override
    public void close() {
        timestampFormat.remove();
    }

    @Override
    public ConfigDef config() {
        return CONFIG_DEF;
    }

}
This is a simple extension of the Apache Kafka [TimestampRouter](https://github.com/apache/kafka/blob/trunk/connect/transforms/src/main/java/org/apache/kafka/connect/transforms/TimestampRouter.java) Single Message Transform (SMT) that 
includes an optional Timezone component.

This is useful when you need to direct messages to time-related destination topics based on the destination system's timezone, where that timezone would cause a difference in hour / day of week / date / month, etc.

Example:

- Kafka message timestamps is 1685534400000 (i.e. stored as epoch offsets, with no timezone info present)
- This message timestamp converts to `Wednesday, 31 May 2023 22:00:00 UTC`
- You need to correctly reflect the date / day of week / month in your timezone, not UTC
- For the timestamp above, if you were somewhere in the world with a UTC+2 to UTC+13 timezone, it would be:
  - 1st day of the month, not the last day
  - Thursday, not Wednesday
  - June, not May
  - 2023-06-01, not 2023-05-31
- Using a timezone aware conversion assists with getting this right.



Example on how to add to your connector:
```
transforms=router
transforms.router.type=name.kel.code.kafka.connect.smt.TimestampRouterWithTz
transforms.router.tz="Australia/Sydney"
transforms.router.timestamp.format="YYYYMMdd"
transforms.router.topic.format="${topic}_${timestamp}"
```

Given the above config, an input topic of `XYZ` and message timestamp (as string) of `Monday, 1 May 2023 22:00:00 GMT` the message will be directed to `XYZ_20230502` and not `XYZ_20230501` as would be the case with the default TimestampRouter SMT.


Todo:
* Validate the supplied timezone code. Invalid timezones will default to `GMT` which may cause issues downstream.



This is a simple extension of the Apache Kafka TimestampRouter Single Message Transform (SMT) that 
includes an optional Timezone component.

This is useful when you need to direct messages to time-related destination topics based on the destination system's timezone, where that timezone would cause a difference in hour / day of week / date / month, etc.

Scenario where this is useful:

. Kafka message timestamps are always stored as epoch offsets (e.g. no timezone info present)
. You need to set the output topic based on the timestamp, but you / your destination system operates in a different timezone
. E.g. Message timestamp is `Monday, 1 May 2023 22:00:00 GMT`, which is actually Tuesday is your timezone.



Example on how to add to your connector:
```
transforms=router
transforms.router.type=name.kel.code.kafka.connect.smt.TimestampRouterWithTz$Value
transforms.router.tz="Australia/Sydney"
transforms.router.timestamp.format="yyyyMMdd"
transforms.router.topic.format="${topic}_${timestamp}"
```

Given the above config, an input topic of `XYZ` and message timestamp (as string) of `Monday, 1 May 2023 22:00:00 GMT` the message will be directed to `XYZ_20230502` and not `XYZ_20230501` as would be the case with the default TimestampRouter SMT.


ToDO
* Validate the supplied timezone code. Invalid timezones will default to `GMT` which may cause issues downstream.



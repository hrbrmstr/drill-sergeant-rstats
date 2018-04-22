# Wiring Up Drill and R (`ODBC`-style)

## Problem

You _really_ want to use ODBC vs JDBC to work with Drill in R.

## Solution

Install the MapR ODBC drivers for Drill, the R `odbc` package and use `dplyr`'s inherent ability to work with ODBC connections.

## Discussion

For some users, getting Java working can be a harrowing experience. For others, getting [ODBC](https://github.com/Microsoft/ODBC-Specification) drivers working on non-Windows platforms can also be a harrowing experience. If you're reading this, you're likely in the camp where ODBC is working fine or you're just morbidly curious about acronyms.

ODBC stands for "Open Database Connectivity" and has ultimately become a (fairly successful) attempt at providing a back-end programmatic agnostic interface to databases. The R [`odbc`](https://github.com/r-dbi/odbc) package provides an interface to ODBC and R that you can use directly or via `dplyr` verbs.

If you're not familiar with ODBC you should press the pause button here and go visit [RStudio's excellent primer](https://db.rstudio.com/) on using databases with R. Read it and install the [`odbc`](https://db.rstudio.com/odbc) package before continuuing.

Back? Good. 

Now, you need to head back over to the Drill documentation and [install the ODBC drivers for your platform](https://drill.apache.org/docs/installing-the-odbc-driver/).

To verify configuration bits are set correctly, do the following:


```r
library(odbc)
library(tidyverse)

odbc::odbcListDrivers() %>%
  filter(str_detect(name, "Drill"))
```

```
##                     name   attribute
## 1 MapR Drill ODBC Driver Description
## 2 MapR Drill ODBC Driver      Driver
##                                            value
## 1                         MapR Drill ODBC Driver
## 2 /Library/mapr/drill/lib/libdrillodbc_sbu.dylib
```
If you do not see "`Drill`" in the output, you need to go back and perform the setup steps again. Please file an issue to this chapter if you would like more provided (in another recipe) on troubleshooting.

You can define ODBC data sources in the configuration files, but is is also possible --- and, recommended by the author --- to configure everything in R for maximum portability.

You can even get an idea of minimum parameters required via helpful error messages when using just a blank configuration:


```r
DBI::dbConnect(
  odbc::odbc(),
  driver = "MapR Drill ODBC Driver" # NOTE: you can also use the full path to the library here
)
## Error: nanodbc/nanodbc.cpp:950: 08001: [MapR][ODBC] (10380) Unable to establish a 
## connection with data source. Missing settings: 
## {[AuthenticationType] [ConnectionType] [ZKClusterID] [ZKQuorum]} 
```

You can find all the possible parameters available [over at the Drill documentation](https://drill.apache.org/docs/odbc-configuration-reference/), but we'll cover how to create a minimally-configured connection to both standalone (embedded) and distributed (even in the single-node distributed configuration we've been using in this book). Since the online Drill documentation is a bit malformed (the table is way too wide and unreadable), I've reformatted/reproduced it here:

- **`AdvancedProperties`**
    - _Default_: `CastAnyToVarchar=true; HandshakeTimeout=5; QueryTimeout=180; TimestampTZDisplayTimezone=local; ExcludedSchemas= sys,INFORMATION_SCHEMA; NumberOfPrefetchBuffers=5`
    - _Description_: Not required. Advanced properties for configuring the driver. You can set custom connection properties by specifying them as advanced properties.   If you specify a property that the driver does not explicitly support, the driver still accepts the property, and passes it to the server for processing.  Separate advanced properties using a semi-colon (;) and then surround all advanced properties in a connection string using braces { and }. For example,  {<property>;<property>; . . .}  In addition, the following Advanced Properties string excludes the schemas named test and abc, sets the timeout to 30 seconds, and sets the time zone to Coordinated Universal Time:HandshakeTimeout=30;QueryTimeout=30;TimestampTZDisplayTimezone=utc;ExcludedSchemas=test,abc.

- **`AuthenticationType`**
    - _Default_: `No Authentication`
    - _Description_: Not required.  This option specifies how the driver authenticates the connection to Drill.   No Authentication: The driver does not authenticate the connection to Drill. Kerberos: The driver authenticates the connection using the Kerberos protocol. Plain: The driver authenticates the connection using a user name and a password.

- **`Catalog`**
    - _Default_: `The default catalog name specified  in the driver's .did file (typically, DRILL).`
    - _Description_: Not required. The name of the synthetic catalog under which all of the schemas/databases are organized. This catalog name is used as the value for SQL_DATABASE_NAME or CURRENT CATALOG.

- **`ConnectionType`**
    - _Default_: `Direct to Drillbit (Direct)`
    - _Description_: Required. This option specifies whether the driver connects to a single server or a ZooKeeper cluster. Direct to Drillbit (Direct): The driver connects to a single Drill server. ZooKeeper Quorum (ZooKeeper): The driver connects to a ZooKeeper cluster.

- **`DelegationUID`**
    - _Default_: `none`
    - _Description_: Not required. If a value is specified for this setting, the driver delegates all operations against Drill to the specified user, rather than to the authenticated user for the connection. This option is applicable only when Plain authentication is enabled.

- **`DisableAsync`**
    - _Default_: `Clear (0)`
    - _Description_: Not required. This option specifies whether the driver supports asynchronous queries.   Enabled (1): The driver does not support asynchronous queries. Disabled (0): The driver supports asynchronous queries. This option is not supported in connection strings or DSNs. Instead, it must be set as a driver-wide property in the mapr.drillodbc.ini file. Settings in that file apply to all connections that use the driver.

- **`Driver`**
    - _Default_: `Drill ODBC Driver on Windows machines or the absolute path of the driver shared object file when installed on a non-Windows machine`
    - _Description_: On Windows, the name of the installed driver (Drill ODBC Driver). On other platforms, the name of the installed driver as specified in odbcinst.ini, or the absolute path of the driver shared object file.

- **`Host`**
    - _Default_: `localhost`
    - _Description_: Required if the ConnectionType property is set to Direct to Drillbit. The IP address or host name of the Drill server.

- **`KrbServiceHost`**
    - _Default_: `none`
    - _Description_: Required for Kerberos authentication. The fully qualified domain name of the Drill server host.

- **`KrbServiceName`**
    - _Default_: `map (default)`
    - _Description_: Required for Kerberos authentication. The Kerberos service principal name of the Drill server. mapr is the default for the Drill ODBC driver.

- **`LogLevel`**
    - _Default_: `OFF (0)`
    - _Description_: Not required. Use this property to enable or disable logging in the driver and to specify the amount of detail included in log files. Only enable logging long enough to capture an issue. Logging decreases performance and can consume a large quantity of disk space.   This option is not supported in connection strings. To configure logging for the Windows driver, you must use the Logging Options dialog box. To configure logging for a non-Windows driver, you must use the mapr.drillodbc.ini file.

- **`LogPath`**
    - _Default_: `none`
    - _Description_: Required if logging is enabled. The full path to the folder where the driver saves log files when logging is enabled. When logging is enabled, the driver produces two log files at the location that you specify in the LogPath property:  driver.log provides a log of driver activities, and  drillclient.log provides a log of Drill client activities.   This option is not supported in connection strings. To configure logging for the Windows driver, you must use the Logging Options dialog box. To configure logging for a non-Windows driver, you must use the mapr.drillodbc.ini file.

- **`Port`**
    - _Default_: `31010`
    - _Description_: Required if the ConnectionType property is set to Direct to Drillbit. The TCP port that the Drill server uses to listen for client connections. Set the TCP port on which the Drill server is listening.

- **`PWD`**
    - _Default_: `none`
    - _Description_: Required if AuthenticationType is Plain (also known as Basic Authentication). The password corresponding to the user name that you provided in the User field (the UID key).

- **`Schema`**
    - _Default_: `none`
    - _Description_: Not required. The name of the database schema to use when a schema is not explicitly specified in a query. You can still issue queries on other schemas by explicitly specifying the schema in the query.

- **`UID`**
    - _Default_: `none`
    - _Description_: Required if AuthenticationType is Plain (also known as Basic authentication). Set UID to a user name.

- **`UseOnlySSPI (on Windows only)`**
    - _Default_: `Clear (0)`
    - _Description_: Not required. This option is available only in the Windows driver. This option specifies how the driver handles Kerberos authentication: either with the SSPI plugin or with MIT Kerberos.   Enabled (1): The driver handles Kerberos authentication by using the SSPI plugin instead of MIT Kerberos by default. Disabled (0): The driver uses MIT Kerberos to handle Kerberos authentication, and only uses the SSPI plugin if the GSSAPI library is not available.

- **`ZKClusterID`**
    - _Default_: `drillbits1`
    - _Description_: Required if the ConnectionType property is set to ZooKeeper Quorum. Set ZKClusterID to the name of the Drillbit cluster to use.

- **`ZKQuorum`**
    - _Default_: `none`
    - _Description_: Required if the ConnectionType property is set to ZooKeeper. Set  ZKQuorum to indicate the server(s) in your ZooKeeper cluster. Separate multiple servers using a comma (,). For example, <IP address>,<IP address>.


First, standalone/embedded:


```r
DBI::dbConnect(
  odbc::odbc(),
  driver = "MapR Drill ODBC DRiver",
  Host = "localhost",
  Port = "31010",
  ConnectionType = "Direct to Drillbit",
  AuthenticationType = "No Authentication",
  ZkClusterID = "drillbits1",
  ZkQuorum = ""
)
## <OdbcConnection> Apache Drill Server
##  Database: DRILL
##  Drill Version: 01.13.0000
```

and, distributed:


```r
DBI::dbConnect(
  odbc::odbc(),
  driver = "MapR Drill ODBC DRiver",
  Host = "localhost",
  Port = "31010",
  ConnectionType = "Zookeeper",
  AuthenticationType = "No Authentication",
  ZKCLusterID = "drillbits1", # use the zookeeper cluster name you chose if it's different
  ZkQuorum = "localhost:2181",
)
## <OdbcConnection> Apache Drill Server
##  Database: DRILL
##  Drill Version: 01.13.0000
```

If you don't see that connection output with the Drill verison you are using, re-read the error message as it often outputs very helpful "you forgot _this_" messages.

You should also spend some time with those `AdvancedProperties` as they may help speed up queries and adjust session settings that might make it easier to work with your data. A recommended baseline configuration is:

    AdvancedProperties = "CastAnyToVarchar=true;HandshakeTimeout=30;QueryTimeout=180;TimestampTZDisplayTimezone=utc;ExcludedSchemas=sys,INFORMATION_SCHEMA;NumberOfPrefetchBuffers=5;"

It has to be one, contiguous line so I let it scroll in the above string to make it easier to copy/paste. Here are the individual element settings:

    CastAnyToVarchar = true
    HandshakeTimeout = 30
    QueryTimeout = 180
    TimestampTZDisplayTimezone = utc
    ExcludedSchemas = sys,INFORMATION_SCHEMA
    NumberOfPrefetchBuffers = 5

We can work with this ODBC connection via the traditional `DBI` method _or_ via `dplyr` verbs. Here are examples of both using the test `employee.json` built-in data set:


```r
library(DBI)
library(odbc)
library(tidyverse)

DBI::dbConnect(
  odbc::odbc(),
  driver = "MapR Drill ODBC DRiver",
  Host = "localhost",
  Port = "31010",
  ConnectionType = "Zookeeper",
  AuthenticationType = "No Authentication",
  ZKCLusterID = "drillbits1", # use the zookeeper cluster name you chose if it's different
  ZkQuorum = "localhost:2181",
  AdvancedProperties = "CastAnyToVarchar=true;HandshakeTimeout=30;QueryTimeout=180;TimestampTZDisplayTimezone=utc;
ExcludedSchemas=sys,INFORMATION_SCHEMA;NumberOfPrefetchBuffers=5;"
) -> drill_con

drill_con
```

```
## <OdbcConnection> Apache Drill Server
##   Database: DRILL
##   Drill Version: 01.13.0000
```

```r
## DBI

str(odbc::dbGetInfo(drill_con))
```

```
## List of 13
##  $ dbname               : chr "DRILL"
##  $ dbms.name            : chr "Drill"
##  $ db.version           : chr "01.13.0000"
##  $ username             : chr ""
##  $ host                 : chr ""
##  $ port                 : chr ""
##  $ sourcename           : chr ""
##  $ servername           : chr "Apache Drill Server"
##  $ drivername           : chr "MapR Drill ODBC Driver"
##  $ odbc.version         : chr "03.52"
##  $ driver.version       : chr "1.3.16.1049"
##  $ odbcdriver.version   : chr "03.80"
##  $ supports.transactions: logi FALSE
##  - attr(*, "class")= chr [1:3] "Drill" "driver_info" "list"
```

```r
odbc::odbcListObjects(drill_con)
```

```
##   name   type
## 1    I schema
## 2    c schema
## 3    d schema
## 4    s schema
```

```r
odbc::odbcPreviewObject(drill_con, 10, "cp.`employee.json`")
```

```
##    employee_id         full_name first_name last_name position_id
## 1            1      Sheri Nowmer      Sheri    Nowmer           1
## 2            2   Derrick Whelply    Derrick   Whelply           2
## 3            4    Michael Spence    Michael    Spence           2
## 4            5    Maya Gutierrez       Maya Gutierrez           2
## 5            6   Roberta Damstra    Roberta   Damstra           3
## 6            7  Rebecca Kanagaki    Rebecca  Kanagaki           4
## 7            8       Kim Brunner        Kim   Brunner          11
## 8            9   Brenda Blumberg     Brenda  Blumberg          11
## 9           10      Darren Stanz     Darren     Stanz           5
## 10          11 Jonathan Murraiin   Jonathan  Murraiin          11
##            position_title store_id department_id birth_date
## 1               President        0             1 1961-08-26
## 2      VP Country Manager        0             1 1915-07-03
## 3      VP Country Manager        0             1 1969-06-20
## 4      VP Country Manager        0             1 1951-05-10
## 5  VP Information Systems        0             2 1942-10-08
## 6      VP Human Resources        0             3 1949-03-27
## 7           Store Manager        9            11 1922-08-10
## 8           Store Manager       21            11 1979-06-23
## 9              VP Finance        0             5 1949-08-26
## 10          Store Manager        1            11 1967-06-20
##                hire_date     salary supervisor_id  education_level
## 1  1994-12-01 00:00:00.0 80000.0000             0  Graduate Degree
## 2  1994-12-01 00:00:00.0 40000.0000             1  Graduate Degree
## 3  1998-01-01 00:00:00.0 40000.0000             1  Graduate Degree
## 4  1998-01-01 00:00:00.0 35000.0000             1 Bachelors Degree
## 5  1994-12-01 00:00:00.0 25000.0000             1 Bachelors Degree
## 6  1994-12-01 00:00:00.0 15000.0000             1 Bachelors Degree
## 7  1998-01-01 00:00:00.0 10000.0000             5 Bachelors Degree
## 8  1998-01-01 00:00:00.0 17000.0000             5  Graduate Degree
## 9  1994-12-01 00:00:00.0 50000.0000             1  Partial College
## 10 1998-01-01 00:00:00.0 15000.0000             5  Graduate Degree
##    marital_status gender   management_role
## 1               S      F Senior Management
## 2               M      M Senior Management
## 3               S      M Senior Management
## 4               M      F Senior Management
## 5               M      F Senior Management
## 6               M      F Senior Management
## 7               S      F  Store Management
## 8               M      F  Store Management
## 9               M      M Senior Management
## 10              S      M  Store Management
```

```r
odbc::dbGetQuery(drill_con, "
SELECT 
  position_title, COUNT(position_title) AS ct 
FROM cp.`employee.json`
GROUP BY position_title
ORDER BY ct DESC
")
```

```
##               position_title  ct
## 1    Store Temporary Checker 268
## 2    Store Temporary Stocker 264
## 3    Store Permanent Checker 226
## 4    Store Permanent Stocker 222
## 5     Store Shift Supervisor  52
## 6    Store Permanent Butcher  32
## 7              Store Manager  24
## 8    Store Assistant Manager  24
## 9  Store Information Systems  16
## 10 HQ Finance and Accounting   8
## 11        VP Country Manager   6
## 12    HQ Information Systems   4
## 13              HQ Marketing   3
## 14        HQ Human Resources   2
## 15                VP Finance   1
## 16        VP Human Resources   1
## 17    VP Information Systems   1
## 18                 President   1
```

```r
## dplyr
employee <- tbl(drill_con, sql("SELECT * FROM cp.`employee.json`"))

employee
```

```
## # Source:   SQL [?? x 16]
## # Database: Drill 01.13.0000[@Apache Drill Server/DRILL]
##    employee_id full_name  first_name last_name position_id position_title 
##    <chr>       <chr>      <chr>      <chr>     <chr>       <chr>          
##  1 1           Sheri Now… Sheri      Nowmer    1           President      
##  2 2           Derrick W… Derrick    Whelply   2           VP Country Man…
##  3 4           Michael S… Michael    Spence    2           VP Country Man…
##  4 5           Maya Guti… Maya       Gutierrez 2           VP Country Man…
##  5 6           Roberta D… Roberta    Damstra   3           VP Information…
##  6 7           Rebecca K… Rebecca    Kanagaki  4           VP Human Resou…
##  7 8           Kim Brunn… Kim        Brunner   11          Store Manager  
##  8 9           Brenda Bl… Brenda     Blumberg  11          Store Manager  
##  9 10          Darren St… Darren     Stanz     5           VP Finance     
## 10 11          Jonathan … Jonathan   Murraiin  11          Store Manager  
## # ... with more rows, and 10 more variables: store_id <chr>,
## #   department_id <chr>, birth_date <chr>, hire_date <chr>, salary <chr>,
## #   supervisor_id <chr>, education_level <chr>, marital_status <chr>,
## #   gender <chr>, management_role <chr>
```

```r
count(employee, position_title, sort=TRUE)
```

```
## # Source:     lazy query [?? x 2]
## # Database:   Drill 01.13.0000[@Apache Drill Server/DRILL]
## # Ordered by: desc(n)
##    position_title            n              
##    <chr>                     <S3: integer64>
##  1 Store Temporary Checker   268            
##  2 Store Temporary Stocker   264            
##  3 Store Permanent Checker   226            
##  4 Store Permanent Stocker   222            
##  5 Store Shift Supervisor    52             
##  6 Store Permanent Butcher   32             
##  7 Store Manager             24             
##  8 Store Assistant Manager   24             
##  9 Store Information Systems 16             
## 10 HQ Finance and Accounting 8              
## # ... with more rows
```

You can use this `odbc`-to-`dplyr` instead of `sergeant` and the vast majority of the rest of this book should work without modification. You can wrap custom `SELECT` queries for `tbl(drill_con, ...)` in `sql()` (as above) to execute any optimized SQL you may have for your own Drill queries.  

One other advantage the `odbc` interface has is that it implicitly supports 64-bit integers coming from Drill via the `bit64` package. 

TODO: List more benefits/caveats

## See Also

- [RStudio database resources](https://db.rstudio.com/)
- [Drill Official ODBC documentation](https://drill.apache.org/docs/interfaces-introduction/)

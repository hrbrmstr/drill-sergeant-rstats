# Wiring Up Drill and R (`RJDBC`-style)

## Problem

You have Drill installed and want to work with Drill from R using `RJDBC` idioms.

## Solution

Install, load and use the `sergeant` package.

## Discussion

Working with Drill in an `RJDBC` context is pretty straightforward. Assuming you have drill running you need to:

- connect to the database
- issue a query

The Drill query interface (<http://localhost:8047/query>) provides an example query we can use for testing if your setup is working:

    SELECT * FROM cp.`employee.json` LIMIT 20

Let's take a look at the `employee.json` table using R & `RJDBC`.



```r
library(sergeant)

con <- drill_jdbc("localhost")

con
```

```
## <JDBCConnection>
```

```r
res <- dbGetQuery(con, "SELECT * FROM cp.`employee.json` LIMIT 20")

str(res)
```

```
## 'data.frame':	20 obs. of  16 variables:
##  $ employee_id    : chr  "1" "2" "4" "5" ...
##  $ full_name      : chr  "Sheri Nowmer" "Derrick Whelply" "Michael Spence" "Maya Gutierrez" ...
##  $ first_name     : chr  "Sheri" "Derrick" "Michael" "Maya" ...
##  $ last_name      : chr  "Nowmer" "Whelply" "Spence" "Gutierrez" ...
##  $ position_id    : chr  "1" "2" "2" "2" ...
##  $ position_title : chr  "President" "VP Country Manager" "VP Country Manager" "VP Country Manager" ...
##  $ store_id       : chr  "0" "0" "0" "0" ...
##  $ department_id  : chr  "1" "1" "1" "1" ...
##  $ birth_date     : chr  "1961-08-26" "1915-07-03" "1969-06-20" "1951-05-10" ...
##  $ hire_date      : chr  "1994-12-01 00:00:00.0" "1994-12-01 00:00:00.0" "1998-01-01 00:00:00.0" "1998-01-01 00:00:00.0" ...
##  $ salary         : chr  "80000.0000" "40000.0000" "40000.0000" "35000.0000" ...
##  $ supervisor_id  : chr  "0" "1" "1" "1" ...
##  $ education_level: chr  "Graduate Degree" "Graduate Degree" "Graduate Degree" "Bachelors Degree" ...
##  $ marital_status : chr  "S" "M" "S" "M" ...
##  $ gender         : chr  "F" "M" "M" "F" ...
##  $ management_role: chr  "Senior Management" "Senior Management" "Senior Management" "Senior Management" ...
```

The R manual page for `drill_jdbc()` informs you that you need to have `DRILL_JDBC_JAR` setup in your environment and for Drill 1.13.0 on a Linux-ish system that should become an entry in your `~/.Renviron` file as so:

    DRILL_JDBC_JAR=/usr/local/drill/jars/jdbc-driver/drill-jdbc-all-1.13.0.jar

On Windows that will be `TBD`.

Odds are that if you're in this section of the book you're familiar with RJDBC operations. You'll also be disappointed that most of the RJDBC interface has not been fully implemented since there have been virtually no requests for it. File an issue if you would like more than just the ability to perform queries.

NOTE: You're _really_ better off using `drill_connection()` and `drill_query()` vs go through the RJDBC machinations since you get the same thing without the overhead of Java.

## See Also

- [Drill package source README](https://github.com/hrbrmstr/sergeant/blob/master/README.md)

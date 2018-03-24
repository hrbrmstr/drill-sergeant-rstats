# Wiring Up Drill and R (`dplyr`-style)

## Problem

You have Drill installed and want to work with Drill from R using `dplyr` idioms.

## Solution

Install, load and use the `sergeant` package.

## Discussion

Working with Drill in a `dplyr` context is pretty straightforward. Assuming you have drill running you need to:

- connect to the database
- identify a table
- perform normal operations

The Drill query interface (<http://localhost:8047/query>) provides an example query we can use for testing if your setup is working:

    SELECT * FROM cp.`employee.json` LIMIT 20

Let's take a look at the `employee.json` table using R & `dplyr`.


```r
library(sergeant)
library(tidyverse)

db <- src_drill("localhost")

db
```

```
## src:  DrillConnection
## tbls: cp.default, dfs.caps, dfs.d, dfs.default, dfs.root, dfs.tmp,
##   INFORMATION_SCHEMA, sys
```

```r
employee <- tbl(db, "cp.`employee.json`")

employee
```

```
## # Source:   table<cp.`employee.json`> [?? x 16]
## # Database: DrillConnection
##    store_id gender department_id birth_date supervisor_id last_name
##       <int> <chr>          <int> <date>             <int> <chr>    
##  1        0 F                  1 1961-08-26             0 Nowmer   
##  2        0 M                  1 1915-07-03             1 Whelply  
##  3        0 M                  1 1969-06-20             1 Spence   
##  4        0 F                  1 1951-05-10             1 Gutierrez
##  5        0 F                  2 1942-10-08             1 Damstra  
##  6        0 F                  3 1949-03-27             1 Kanagaki 
##  7        9 F                 11 1922-08-10             5 Brunner  
##  8       21 F                 11 1979-06-23             5 Blumberg 
##  9        0 M                  5 1949-08-26             1 Stanz    
## 10        1 M                 11 1967-06-20             5 Murraiin 
## # ... with more rows, and 10 more variables: position_title <chr>,
## #   hire_date <dttm>, management_role <chr>, salary <dbl>,
## #   marital_status <chr>, full_name <chr>, employee_id <int>,
## #   education_level <chr>, first_name <chr>, position_id <int>
```

If you've never worked with the `dplyr` and databases before, you may not be aware that what's really happening is different than what you experince with local R data frames.

The call to `src_drill("localhost")` does some RJDBC/DBI "magic" behind the scenes to setup all the necessary connection parameters.

By printing `db`, a SQL query ---`SHOW DATABASES`---is issued to get available storage/databases and reformats it to conform to the expected `dplyr` datbase API.

When `tbl(...)` is executed a

    SELECT * FROM  cp.`employee.json`  LIMIT 1

is issued to obtain field information for future operations on `employee`.

Finally, when `employee` is printed, the query

    SELECT * FROM  cp.`employee.json`  LIMIT 10

is issued and the contents transferred back into R. `sergeant` cheats a bit since the data is run through `readr::type_convert()` before being delivered back to the user. It does this for a whole host of reasons, but mostly out of convenience for the user.

## See Also

- [Drill package source README](https://github.com/hrbrmstr/sergeant/blob/master/README.md)

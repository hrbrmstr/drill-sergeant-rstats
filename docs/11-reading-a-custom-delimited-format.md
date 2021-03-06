# Working With Custom Delimited Format Files


## Problem

You have a custom delimited file format that you want to be able to work with Drill and R.

## Solution

Add a new entries to Drill storage `workspaces` and `formats` and use custom queries to have sane data types.

## Discussion

A helpful chap---[Ben Marwick](https://github.com/benmarwick)---provided an example of a truly evil set of delimited files from the [Wikimedia Commons](https://github.com/hrbrmstr/drill-sergeant-rstats/issues/1#issuecomment-376713894) that have pagecount data for all Wikimedia properties. The files are _huge_ so we'll be working with an extract. The link to the sample extract file will be posted soon, but it was created with:

    $ bzcat pagecounts-2012-12-01.bz2 | head -1000024 > pagecounts-2012-02-01-excerpt.csvw
    $ bzip2 --compress pagecounts-2012-02-01-excerpt.csvw

You'll find out the reason for the `csvw` in a bit.

The files all look like this:

    # Wikimedia page request counts for 01/12/2012 (dd/mm/yyyy)
    #
    # Each line shows 'project page daily-total hourly-counts'
    #
    # Project is 'language-code project-code'
    #
    # Project-code is
    #
    # b:wikibooks,
    # k:wiktionary,
    # n:wikinews,
    # q:wikiquote,
    # s:wikisource,
    # v:wikiversity,
    # z:wikipedia (z added by merge script: wikipedia happens to be sorted last in dammit.lt files, but without suffix)
    #
    # Counts format: only hours with page view count > 0 (or data missing) are represented,
    #
    # Hour 0..23 shown as A..X (saves up to 22 bytes per line compared to comma separated values), followed by view count.
    # If data are missing for some hour (file missing or corrupt) a question mark (?) is shown,
    # and a adjusted daily total is extrapolated as follows: for each missing hour the total is incremented with hourly average
    #
    # Page titles are shown unmodified (preserves sort sequence)
    #
    Ar.mw Ar 5 M3R2
    De.mw De 3 T3
    En.mw En 3 E1F2
    aa.b ?banner=B12_5C_113020_hover_nohover 11 A11
    aa.b File:Broom_icon.svg 2 D1X1
    aa.b File:Commons-logo.svg 1 X1

Just with another 65 _million_ lines (or so) per file. 

They come [bzipped](http://bzip.org/) and Drill can definitely handle files with the `bz2` extension, but we are going to rename these files to help ensure we recognize the delimited encoding type when we browse the file system and when constructing queries.

The comments in the data files are helpful since they tell us the format:

- comment character is `#`
- space `' '` is the delimiter
- `project` is really `language-code` and `project-code` separated by a `.` (which we'll deal with here)
- there's a wonky bit of encoding in the `hourly-counts` (which we'll deal with in another recipe)

We're going to store all these pagecount files (we only have one due to size) in a `~/Data/wikimedia` directory so we can apply an input storage format directory-wide (this isn't 100% necessary but an organized filesystem is a happy filesystem) and create a special storage format for these files.

You'll need to go to <http://localhost:8047/storage> and "update" the `dfs` storage plugin so we can add these new entries. While the editor has syntax checking, it is recommended that you either use a browser that lets you change the size of the input text area box _or_ use an external text editor to do this work.

Find the line that has:

    "workspaces": {

and add a new workspace, which is (for this filesystem example) just a `dfs` shortcut name to a location on the filesystem:

    "wikimedia": {
      "location": "/Users/bob/Data/wikimedia",
      "writable": true,
      "defaultInputFormat": "wikicsv",
      "allowAccessOutsideWorkspace": false
    },

This is JSON so the ending `,` is necessary unless it's the last entry in `workspaces`. You'll (again) need to change the specific path to the place where you've downloaded the file. We've taken a chance and made this directory writeable and told Drill that the default format will be "`wikicsv`" files, which we'll define next.

Now, find the line that starts with:

    "formats": {

and add an entry for our new "`wikicsv`" format.

    "wikicsv": {
      "type": "text",
      "extensions": [
        "csvw"
      ],
      "delimiter": " "
    },

The default comment character for delimited files is "`#`" so there's no need to save that.

Save the configuration update and head over to <http://localhost:8047/query> (or use `drill-conf` on the command-line) to test out the new setup.

The example pagecount file is named `pagecounts-2012-02-01-excerpt.csvw.bz2` which lets Drill know it's a `wikimedia` delimited file and also that it's bzip compressed. 

Let's try a query:

    0: jdbc:drill:> SELECT * FROM dfs.wikimedia.`/*.csvw.bz2` LIMIT 10;
    +------------------------------------------------------------+
    |                          columns                           |
    +------------------------------------------------------------+
    | ["Ar.mw","Ar","5","M3R2"]                                  |
    | ["De.mw","De","3","T3"]                                    |
    | ["En.mw","En","3","E1F2"]                                  |
    | ["aa.b","?banner=B12_5C_113020_hover_nohover","11","A11"]  |
    | ["aa.b","File:Broom_icon.svg","2","D1X1"]                  |
    | ["aa.b","File:Commons-logo.svg","1","X1"]                  |
    | ["aa.b","File:Incubator-notext.svg","5","C1G1V1X2"]        |
    | ["aa.b","File:Wikibooks-logo.svg","1","X1"]                |
    | ["aa.b","File:Wikimania.svg","1","X1"]                     |
    | ["aa.b","File:Wikimedia-logo.svg","1","X1"]                |
    +------------------------------------------------------------+
    10 rows selected (0.268 seconds)

While it's not _exactly_ what we want, the good news is that the headers were skipped and the file decompressed perfectly. However, we'll need to use `columns[#]` to help get to the data in each column. Let's try that again:

    0: jdbc:drill:> SELECT columns[0], columns[1], columns[2], columns[3] FROM dfs.wikimedia.`/*.csvw.bz2` LIMIT 10;
    +---------+--------------------------------------+---------+-----------+
    | EXPR$0  |                EXPR$1                | EXPR$2  |  EXPR$3   |
    +---------+--------------------------------------+---------+-----------+
    | Ar.mw   | Ar                                   | 5       | M3R2      |
    | De.mw   | De                                   | 3       | T3        |
    | En.mw   | En                                   | 3       | E1F2      |
    | aa.b    | ?banner=B12_5C_113020_hover_nohover  | 11      | A11       |
    | aa.b    | File:Broom_icon.svg                  | 2       | D1X1      |
    | aa.b    | File:Commons-logo.svg                | 1       | X1        |
    | aa.b    | File:Incubator-notext.svg            | 5       | C1G1V1X2  |
    | aa.b    | File:Wikibooks-logo.svg              | 1       | X1        |
    | aa.b    | File:Wikimania.svg                   | 1       | X1        |
    | aa.b    | File:Wikimedia-logo.svg              | 1       | X1        |
    +---------+--------------------------------------+---------+-----------+
    10 rows selected (0.18 seconds)

Better! However, those computed column names will have to go and we also need to do something about the first column since it's really two columns compressed together. Renaming is straightforward (the `sqlline` prefixes will be dropped from now on to make it easier to copy/past the SQL queries; just assume it's being used in a Drill web query box or `sqlline` prompt):

    SELECT 
      columns[0] AS project,
      columns[1] AS page,
      CAST(columns[2] AS DOUBLE) AS daily_total,
      columns[3] AS hr_ct
    FROM dfs.wikimedia.`/*.csvw.bz2` LIMIT 10 
    +----------+--------------------------------------+--------------+-----------+
    | project  |                 page                 | daily_total  |   hr_ct   |
    +----------+--------------------------------------+--------------+-----------+
    | Ar.mw    | Ar                                   | 5.0          | M3R2      |
    | De.mw    | De                                   | 3.0          | T3        |
    | En.mw    | En                                   | 3.0          | E1F2      |
    | aa.b     | ?banner=B12_5C_113020_hover_nohover  | 11.0         | A11       |
    | aa.b     | File:Broom_icon.svg                  | 2.0          | D1X1      |
    | aa.b     | File:Commons-logo.svg                | 1.0          | X1        |
    | aa.b     | File:Incubator-notext.svg            | 5.0          | C1G1V1X2  |
    | aa.b     | File:Wikibooks-logo.svg              | 1.0          | X1        |
    | aa.b     | File:Wikimania.svg                   | 1.0          | X1        |
    | aa.b     | File:Wikimedia-logo.svg              | 1.0          | X1        |
    +----------+--------------------------------------+--------------+-----------+
    10 rows selected (1.039 seconds)

Along with having better column names we also took the opportunity to ensure `daily_total` is a numeric type vs a character. The `hr_ct` column conversion will be covered in another recipe (we'll need to make a Drill User Defined Function [UDF] for it) but we can handle separating `project` out here:

    SELECT 
      SUBSTR(columns[0], 1, STRPOS(columns[0], '.')-1) AS language_code,
      SUBSTR(columns[0], STRPOS(columns[0], '.')+1) AS project_code,
      columns[1] AS page,
      CAST(columns[2] AS DOUBLE) AS daily_total,
      columns[3] AS hr_ct
    FROM dfs.wikimedia.`/*.csvw.bz2` LIMIT 10
    +----------------+---------------+--------------------------------------+--------------+-----------+
    | language_code  | project_code  |                 page                 | daily_total  |   hr_ct   |
    +----------------+---------------+--------------------------------------+--------------+-----------+
    | Ar             | mw            | Ar                                   | 5.0          | M3R2      |
    | De             | mw            | De                                   | 3.0          | T3        |
    | En             | mw            | En                                   | 3.0          | E1F2      |
    | aa             | b             | ?banner=B12_5C_113020_hover_nohover  | 11.0         | A11       |
    | aa             | b             | File:Broom_icon.svg                  | 2.0          | D1X1      |
    | aa             | b             | File:Commons-logo.svg                | 1.0          | X1        |
    | aa             | b             | File:Incubator-notext.svg            | 5.0          | C1G1V1X2  |
    | aa             | b             | File:Wikibooks-logo.svg              | 1.0          | X1        |
    | aa             | b             | File:Wikimania.svg                   | 1.0          | X1        |
    | aa             | b             | File:Wikimedia-logo.svg              | 1.0          | X1        |
    +----------------+---------------+--------------------------------------+--------------+-----------+
    10 rows selected (0.188 seconds)

Now, we can try that from R with `sergeant`:


```r
library(sergeant)
library(tidyverse)

db <- src_drill("localhost")

# remember we need to wrap this custom query in `()`
tbl(db, "(
SELECT 
  SUBSTR(columns[0], 1, STRPOS(columns[0], '.')-1) AS language_code,
  SUBSTR(columns[0], STRPOS(columns[0], '.')+1) AS project_code,
  columns[1] AS page,
  CAST(columns[2] AS DOUBLE) AS daily_total,
  columns[3] AS hr_ct
FROM dfs.wikimedia.`/*.csvw.bz2`
)") -> pagecount

pagecount
```

```
## # Source:   table<( SELECT SUBSTR(columns[0], 1, STRPOS(columns[0],
## #   '.')-1) AS language_code, SUBSTR(columns[0], STRPOS(columns[0],
## #   '.')+1) AS project_code, columns[1] AS page, CAST(columns[2] AS
## #   DOUBLE) AS daily_total, columns[3] AS hr_ct FROM
## #   dfs.wikimedia.`/*.csvw.bz2` )> [?? x 5]
## # Database: DrillConnection
##    language_code daily_total hr_ct    project_code page                   
##    <chr>               <dbl> <chr>    <chr>        <chr>                  
##  1 Ar                     5. M3R2     mw           Ar                     
##  2 De                     3. T3       mw           De                     
##  3 En                     3. E1F2     mw           En                     
##  4 aa                    11. A11      b            ?banner=B12_5C_113020_…
##  5 aa                     2. D1X1     b            File:Broom_icon.svg    
##  6 aa                     1. X1       b            File:Commons-logo.svg  
##  7 aa                     5. C1G1V1X2 b            File:Incubator-notext.…
##  8 aa                     1. X1       b            File:Wikibooks-logo.svg
##  9 aa                     1. X1       b            File:Wikimania.svg     
## 10 aa                     1. X1       b            File:Wikimedia-logo.svg
## # ... with more rows
```

```r
count(pagecount, language_code, sort=TRUE) %>% 
  collect() %>% 
  print(n=23)
```

```
## # A tibble: 23 x 2
##    language_code      n
##  * <chr>          <int>
##  1 ar            576618
##  2 az             88845
##  3 af             67132
##  4 an             46220
##  5 als            28824
##  6 ast            27216
##  7 arz            25133
##  8 am             20915
##  9 bar            19659
## 10 ba             18648
## 11 ang            16531
## 12 as             14390
## 13 bat-smg        11806
## 14 arc             9524
## 15 ace             9301
## 16 av              7092
## 17 ab              6177
## 18 ay              3720
## 19 ak              1522
## 20 aa               724
## 21 En                 1
## 22 De                 1
## 23 Ar                 1
```

As noted, converting the `hr_ct` column is complex enough that it needs its own recipe. And, take note of Recipe `TBD` that shows how to convert these delimited files into parquet files which can really speed up processing and reduce memory requirements.

NOTE: These Wikimedia Commons pagecount files would work best converted to parquet files and stored in in a small Drill cluster but they can be handled on a single-node system with the right configuration.

## See Also

- [Configure Drill Introduction](https://drill.apache.org/docs/configure-drill-introduction/)
- [Text Files: CSV, TSV, PSV](https://drill.apache.org/docs/text-files-csv-tsv-psv/)

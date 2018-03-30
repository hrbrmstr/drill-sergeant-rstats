# Writing "Simple" Drill Custom Functions (UDFs) For Field Transformations


## Problem

You have a field that requires complex transformations in order to be useful in a Drill context.

## Solution

Write a Drill custom function/user-defined function (UDF)

## Discussion

In Recipe `TBD` we came across a field that holds encoded hourly counts of pageviews for Wikimedia property pages. The encoding is a contiguous string that uses a series of single A-Z characters to encode hours 00:23 followed by an integer value. This would be an example of such a string:

    M3R20A131Q27H53

It's _possible_ to handle this with some _serious_ SQL machinations, but we'd likely go insane in the process. An alternative is to write a Drill user-defined function (UDF) that handles some of this for us. One big hurdle for us is that UDFs are written in Java. So, you will either need _some_ Java familiarity to get through this recipe _or_ offer up some adult beverages in exchange for some assistance from someone who can tolerate the verbosity and idioms of Java.

An _absolute_ prerequisite for this recipe is a _complete_ review of the Drill manual chapter on [user-defined functions](https://drill.apache.org/docs/develop-custom-functions-introduction/). It doesn't make much sense to duplicate that content, but we'll draw on concepts in that example for this solution.

Drill UDF "projects" define naming conventions, extra Java library dependencies, UDF incoming parameters (including types) and the UDF return value(s). We're going to keep it simple for this example and take in a value such as:

    M3R20A131Q27H53

and return back a JSON string like:

    [{"hr":12,"hr_ct":3},{"hr":17,"hr_ct":20},{"hr":0,"hr_ct":131},{"hr":16,"hr_ct":27},{"hr":7,"hr_ct":53}]

which will enable us to work with standard Drill functions for further transformations.

Star by cloning the [example UDF repo](https://github.com/tgrall/drill-simple-mask-function) from the [tutorial](https://drill.apache.org/docs/tutorial-develop-a-simple-function/) and renaming it for this example (we'll assume you're in your "development" directory because you keep things organized, right?):

    $ git clone https://github.com/tgrall/drill-simple-mask-function.git
    $ mv drill-simple-mask-function/ pagecount-hour-count-split

In the top-level of that new directory there's a `pom.xml` file which holds all the metadata about this project. We're only going to change a few bits of this metadata since we're not going to add in any other dependencies. Change each of these XML tags to look like this:

    <artifactId>pagecount-hour-count-split</artifactId>
    <version>1.0</version>
    <name>Drill Pagecount Hour Count Splitter Function</name>

We also need to modify some configuration information in `src/main/resources/drill-module.conf` since we are going to have our own class name for our new UDF. Make that file look like:

    drill {

      classpath.scanning {
        base.classes : ${?drill.classpath.scanning.base.classes} [
          org.apache.drill.contrib.function.ConvertPageCountFunc
        ],
        packages : ${?drill.classpath.scanning.packages} [
          org.apache.drill.contrib.function
        ]
      }
    }

`org.apache.drill.contrib.function.ConvertPageCountFunc` is going to be our new function. But, we'll first need to rename the file `SimpleMaskFunc.java` in `src/main/java/org/apache/drill/contrib/function/` to `ConvertPageCountFunc.java`. 

We'll edit the file to look like this:

    package org.apache.drill.contrib.function;

    import com.google.common.base.Strings;
    import io.netty.buffer.DrillBuf;
    import org.apache.drill.exec.expr.DrillSimpleFunc;
    import org.apache.drill.exec.expr.annotations.FunctionTemplate;
    import org.apache.drill.exec.expr.annotations.Output;
    import org.apache.drill.exec.expr.annotations.Param;
    import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
    import org.apache.drill.exec.expr.holders.VarCharHolder;

    import javax.inject.Inject;

    import java.util.regex.Matcher;
    import java.util.regex.Pattern;

    @FunctionTemplate(
      name = "pagecounts_to_json", // THIS IS WHAT WE'LL USE IN Drill QUERIES TO TRANSFORM THE DATA
      scope = FunctionTemplate.FunctionScope.SIMPLE,
      nulls = FunctionTemplate.NullHandling.NULL_IF_NULL
      )
    public class ConvertPageCountFunc implements DrillSimpleFunc {

      @Param
      NullableVarCharHolder input; // ONLY ONE INPUT PARAMETER (i.e. THE COLUMN WE'RE PASSING IN)

      @Output
      VarCharHolder out; // ONLY ONE OUTPUT VALUE (i.e. THE TRANSFORMED DATA TO JSON)

      @Inject
      DrillBuf buffer;

      public void setup() {}

      public void eval() {

        // get the pagecount compacted hour-count string
        String inputValue = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(input.start, input.end, input.buffer);

        // this pattern will match all valid hour-count entries (and deal with "?")
        java.util.regex.Pattern hcList = java.util.regex.Pattern.compile("([A-Xa-x][0-9]+|[A-Xa-x]\\?)");
        java.util.regex.Matcher hcListMatcher = hcList.matcher(inputValue);

        // this pattern will match individual components of hour-count entries
        java.util.regex.Pattern hc = java.util.regex.Pattern.compile("([A-Xa-x])([0-9]+|\\?)");
        java.util.regex.Matcher hc_entry; // used in the while loop

        String outputValue = "["; // start of 'json'
        
        // for each hr-ct match:
        //   - extract hr
        //   - extract ct; if ct is ? then ct is null

        while (hcListMatcher.find()) {
          
          hc_entry = hc.matcher(hcListMatcher.group());
          
          hc_entry.find();
          outputValue = outputValue + "{\"hr\":" + (int)(Character.toUpperCase(hc_entry.group(1).charAt(0))-65) + ",\"hr_ct\":";
          outputValue = outputValue + (hc_entry.group(2) == "?" ? "null" : hc_entry.group(2)) + "},";
          
        }
        
        outputValue = outputValue + "]"; // end of json
        outputValue = outputValue.replace(",]", "]"); // pesky trailing ,

        // put the output value in the out buffer
        out.buffer = buffer;
        out.start = 0;
        out.end = outputValue.getBytes().length;
        buffer.setBytes(0, outputValue.getBytes());

      }

    }

Even if you're not super-familiar with Java the above code should be pretty readable. We're taking in a string and returning a string. We made things easier since there are no dependencies on third-party Java classes that don't ship with Drill. One thing folks who are familiar with Java will notice is that we fully qualify virtually all of the named classes. That's due to the context in which Drill is running these functions. It's a tiny bit of developer-ionconvenience for some performance and execution assurance benefits.

Once we're done editing that file, fire up a command prompt and ensure you're in the UDF top-level directory. Then run the following:

    $ mvn clean package # there should be no errors

    $ # make our new functions accessible by Drill
    $ cp target/pagecount-hour-count-split*.jar /usr/local/drill/jars/3rdparty/ 

    $ drillbit.sh restart # restart Drill

Drill should startup without issue. Now we can do something like the following in the Drill query web interface or from a `sqlline` prompt:

    SELECT 
      language_code, 
      project_code, 
      page, 
      daily_total, 
      b.hc.hr AS hr, 
      b.hc.hr_ct AS hr_ct 
    FROM (
      SELECT 
        language_code, 
        project_code, 
        page, 
        daily_total, 
        FLATTEN(a.hr_ct) AS hc 
      FROM (
        SELECT 
          SUBSTR(columns[0], 1, STRPOS(columns[0], '.')-1) AS language_code,
          SUBSTR(columns[0], STRPOS(columns[0], '.')+1) AS project_code,
          columns[1] AS page,
          CAST(columns[2] AS DOUBLE) AS daily_total,
          convert_fromJSON(pagecounts_to_json(columns[3])) AS hr_ct
        FROM dfs.wikimedia.`/*.csvw.bz2` 
        LIMIT 10) a
      ) b
    +----------------+---------------+--------------------------------------+--------------+-----+--------+
    | language_code  | project_code  |                 page                 | daily_total  | hr  | hr_ct  |
    +----------------+---------------+--------------------------------------+--------------+-----+--------+
    | Ar             | mw            | Ar                                   | 5.0          | 12  | 3      |
    | Ar             | mw            | Ar                                   | 5.0          | 17  | 2      |
    | De             | mw            | De                                   | 3.0          | 19  | 3      |
    | En             | mw            | En                                   | 3.0          | 4   | 1      |
    | En             | mw            | En                                   | 3.0          | 5   | 2      |
    | aa             | b             | ?banner=B12_5C_113020_hover_nohover  | 11.0         | 0   | 11     |
    | aa             | b             | File:Broom_icon.svg                  | 2.0          | 3   | 1      |
    | aa             | b             | File:Broom_icon.svg                  | 2.0          | 23  | 1      |
    | aa             | b             | File:Commons-logo.svg                | 1.0          | 23  | 1      |
    | aa             | b             | File:Incubator-notext.svg            | 5.0          | 2   | 1      |
    | aa             | b             | File:Incubator-notext.svg            | 5.0          | 6   | 1      |
    | aa             | b             | File:Incubator-notext.svg            | 5.0          | 21  | 1      |
    | aa             | b             | File:Incubator-notext.svg            | 5.0          | 23  | 2      |
    | aa             | b             | File:Wikibooks-logo.svg              | 1.0          | 23  | 1      |
    | aa             | b             | File:Wikimania.svg                   | 1.0          | 23  | 1      |
    | aa             | b             | File:Wikimedia-logo.svg              | 1.0          | 23  | 1      |
    +----------------+---------------+--------------------------------------+--------------+-----+--------+
    16 rows selected (0.427 seconds) 

Most of the "magic" happens in this part of the first `SELECT`:

    convert_fromJSON(pagecounts_to_json(columns[3])) AS hr_ct

You should recognize `pagecounts_to_json()` as our new UDF! We pass in the last column to it for transformation. We also deliberately returned a valid JSON array so we could use `FLATTEN()` on it which turns one record into multiple records (as many as are in the array). From there we can use [standard Drill idioms](https://drill.apache.org/docs/querying-complex-data-introduction/) for querying the JSON data in the "atomic" records.

>SQL queries like that one can really make on appreciate the abstractions afforded in "normal" `dplyr` use in R.

You can find the working UDF source code tree in the `extra` directory of this book's GitHub repository.

## See Also

- [Apache Maven](https://maven.apache.org/)
- [Develop Drill Custom Functions](https://drill.apache.org/docs/develop-custom-functions-introduction/)
- [Apache Drill - CUstom Function](https://www.tutorialspoint.com/apache_drill/apache_drill_custom_function.htm)
- [Querying Complex Data](https://drill.apache.org/docs/querying-complex-data-introduction/)

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
  name = "pagecounts_to_json",
  scope = FunctionTemplate.FunctionScope.SIMPLE,
  nulls = FunctionTemplate.NullHandling.NULL_IF_NULL
  )
public class ConvertPageCountFunc implements DrillSimpleFunc {

  @Param
  NullableVarCharHolder input;

  @Output
  VarCharHolder out;

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



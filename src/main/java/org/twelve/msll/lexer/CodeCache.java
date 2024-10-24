package org.twelve.msll.lexer;

import java.util.ArrayList;
import java.util.List;

public class CodeCache {
    private List<String> lines = new ArrayList<>();
    private boolean isMultiLine = false;

    public String getLine(int lineNum) {
        if(lineNum>=this.lines.size()){
            return "";
        }else {
            return this.lines.get(lineNum);
        }
    }

    public int addLine(String line) {
        line = line.trim();
        if (this.isMultiLine) {
            this.isMultiLine = !line.trim().endsWith("*/");
            String appended = this.lines.remove(this.lines.size() - 1) +"\n"+ line;
            this.lines.add(appended);
        } else {
            this.isMultiLine = line.startsWith("/*");
            this.lines.add(line);
        }
        return this.lines.size() - 1;
    }

    public boolean isMultiLine() {
        return this.isMultiLine;
    }
}

package com.aonescan.scanner.Model;

public class Pdf {
    public String fileName;
    public String absPath;
    public long dateModified;
    public String fileSize;


    public Pdf(String filename, String absPath, long dateModified, String fileSize) {
        this.fileName = filename;
        this.absPath = absPath;
        this.dateModified = dateModified;
        this.fileSize = fileSize;
    }

}

package com.jackie.weixinimage;

/**
 * Created by Law on 2015/12/16.
 */
public class FolderBean {
    private String dirPath;//文件夹绝对路径
    private String firstImgPath;
    private String dirName;//文件夹相对路径
    private int count;

    public FolderBean() {
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
        setDirName(dirPath.substring(dirPath.lastIndexOf("/")));
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getDirName() {
        return dirName;
    }

    public void setDirName(String dirName) {
        this.dirName = dirName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

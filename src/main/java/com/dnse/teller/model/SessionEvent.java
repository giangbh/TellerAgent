package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionEvent {
    private int seq;
    private String at;
    private String type;
    private String title;
    private String detail;

    public SessionEvent() {}
    public SessionEvent(int seq, String at, String type, String title, String detail) {
        this.seq = seq;
        this.at = at;
        this.type = type;
        this.title = title;
        this.detail = detail;
    }

    public int getSeq() { return seq; }
    public void setSeq(int seq) { this.seq = seq; }

    public String getAt() { return at; }
    public void setAt(String at) { this.at = at; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}

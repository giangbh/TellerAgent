package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Approvals {
    private boolean customer = false;
    private boolean teller = false;
    private boolean supervisor = false;

    public Approvals() {}
    public Approvals(boolean customer, boolean teller, boolean supervisor) {
        this.customer = customer;
        this.teller = teller;
        this.supervisor = supervisor;
    }

    public boolean isCustomer() { return customer; }
    public void setCustomer(boolean customer) { this.customer = customer; }

    public boolean isTeller() { return teller; }
    public void setTeller(boolean teller) { this.teller = teller; }

    public boolean isSupervisor() { return supervisor; }
    public void setSupervisor(boolean supervisor) { this.supervisor = supervisor; }
}

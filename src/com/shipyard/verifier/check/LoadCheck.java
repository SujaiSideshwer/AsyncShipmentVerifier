package com.shipyard.verifier.check;

import com.shipyard.model.ShippingLoad;

import java.util.Optional;

//Chain of Responsibility pattern - handler base class
//verification - series of independent rules instead of one giant if-else block
//each rule=one handler; handler - finds a problem and returns rejection reason (or) passes load to next handler
//hence, we can easily add/remove/reorder checks with this chain
public abstract class LoadCheck {
    private LoadCheck next;

    public LoadCheck linkTo(LoadCheck next){
        this.next = next;
        return next;
    }

    //returns an empty Optional if load cleared every check, else if there was a rejection of load, it returns a problem description
    public Optional<String> verify(ShippingLoad load){
        Optional<String> problem = inspect(load);
        if(problem.isPresent()){
            return problem;
        }
        return (next == null) ? Optional.empty() : next.verify(load);
    }

    protected abstract Optional<String> inspect(ShippingLoad load);

    protected static boolean isBlank(String s){
        return s == null || s.isBlank();
    }
}

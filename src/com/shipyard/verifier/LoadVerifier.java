package com.shipyard.verifier;

import com.shipyard.model.LoadCategory;
import com.shipyard.model.Priority;
import com.shipyard.model.ShippingLoad;
import com.shipyard.verifier.check.DestinationCheck;
import com.shipyard.verifier.check.LoadCheck;
import com.shipyard.verifier.check.LoadIdCheck;
import com.shipyard.verifier.check.WeightCheck;
import com.shipyard.verifier.strategy.ClassificationStrategy;
import com.shipyard.verifier.strategy.FlagBasedClassificationStrategy;

import java.util.Optional;

//Business logic - combines Chain of Responsibility and Strategy patterns
//CoR - for validation of load; Strategy - for classification of load
public class LoadVerifier {
    private final LoadCheck checks;
    private final ClassificationStrategy strategy;

    public LoadVerifier() {
        this(buildDefaultChain(), new FlagBasedClassificationStrategy());
    }

    public LoadVerifier(LoadCheck checks, ClassificationStrategy strategy) {
        this.checks = checks;
        this.strategy = strategy;
    }

    public RoutingDecision verify(ShippingLoad load){
        Optional<String> problem = checks.verify(load); //run validation chain; non-null means some handler rejected
        if(problem.isPresent()){
            return RoutingDecision.reject(problem.get());
        }
        LoadCategory category = strategy.categorize(load); //classify now valid chain using pluggable strategy
        Priority priority = load.isUrgent() ? Priority.URGENT : Priority.STANDARD;
        return RoutingDecision.route(category, priority);
    }

    private static LoadCheck buildDefaultChain(){
        LoadCheck head = new LoadIdCheck();
        head.linkTo(new DestinationCheck())
                .linkTo(new WeightCheck());
        return head;
    }
}

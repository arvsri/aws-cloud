package in.arvindsri82.cloud;

import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;

public class InstanceRefreshOnSNSEvent implements RequestHandler<SNSEvent, String> {

    public String handleRequest(SNSEvent snsEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Event received ..");
        logger.log(snsEvent.toString());

        var client = AmazonAutoScalingClient.builder().build();

        var asgName = getAutoScalingGroupName(client);
        logger.log("Autoscaling group name " + asgName);

        var request = new StartInstanceRefreshRequest();
        request.setAutoScalingGroupName(asgName);
        request.setStrategy("Rolling");

        var refreshPreference = new RefreshPreferences();
        refreshPreference.setMinHealthyPercentage(50);
        refreshPreference.setInstanceWarmup(300);
        request.setPreferences(refreshPreference);

        client.startInstanceRefresh(request);
        logger.log("Event Processed ..");

        return "Success";
    }

    private String getAutoScalingGroupName(AmazonAutoScaling client) {
        var asgName = System.getenv("ASG_NAME");
        if (asgName == null || asgName.trim().length() == 0) {
            var asgTag = System.getenv("ASG_TAG_VALUE");

            var filter = new Filter().withName("tag-value").withValues(asgTag);
            var request = new DescribeAutoScalingGroupsRequest();
            request.getFilters().add(filter);

            var result = client.describeAutoScalingGroups(request);
            asgName = result.getAutoScalingGroups().get(0).getAutoScalingGroupName();
        }
        return asgName;
    }

}

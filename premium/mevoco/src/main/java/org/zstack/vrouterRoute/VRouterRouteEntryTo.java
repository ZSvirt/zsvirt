package org.zstack.vrouterRoute;

/**
 * Created by weiwang on 19/06/2017.
 */
public class VRouterRouteEntryTo {
    private String destination;

    private String target;

    private Integer distance;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (this.getTarget() != null) {
            sb.append(String.format("\n%s via %s distance %s", this.getDestination(), this.getTarget(), this.getDistance()));
        } else {
            sb.append(String.format("\n%s blackhole distance %s", this.getDestination(), this.getDistance()));
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof VRouterRouteEntryTo)) {
            return false;
        }

        VRouterRouteEntryTo rto = (VRouterRouteEntryTo)obj;
        return rto.getDestination().equals(this.getDestination())
                && rto.getTarget().equals(this.getTarget())
                && rto.getDistance().equals(this.getDistance());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public static VRouterRouteEntryTo valueOf(VRouterRouteEntryVO vo) {
        VRouterRouteEntryTo to = new VRouterRouteEntryTo();
        to.setDestination(vo.getDestination());
        to.setTarget(vo.getTarget());
        to.setDistance(vo.getDistance());

        return to;
    }
}

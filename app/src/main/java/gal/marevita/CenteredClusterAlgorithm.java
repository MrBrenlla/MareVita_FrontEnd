package gal.marevita;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm;
import com.google.maps.android.clustering.algo.StaticCluster;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CenteredClusterAlgorithm<T extends ClusterItem> extends NonHierarchicalDistanceBasedAlgorithm<T> {

    private final int baseMaxDistance;

    public CenteredClusterAlgorithm(int baseMaxDistance) {
        this.baseMaxDistance = baseMaxDistance;
    }

    @Override
    public Set<? extends Cluster<T>> getClusters(float zoom) {

        Set<? extends Cluster<T>> clusters = super.getClusters(zoom);

        Set<Cluster<T>> newClusters = new HashSet<>();

        for (Cluster<T> cluster : clusters) {
            Collection<T> items = cluster.getItems();

            StaticCluster<T> newCluster = new StaticCluster<>(calculateCenter(items));
            for (T item : items) {
                newCluster.add(item);
            }

            newClusters.add(newCluster);
        }

        return newClusters;
    }

    private LatLng calculateCenter(Collection<T> items) {
        double latSum = 0;
        double lngSum = 0;
        for (T item : items) {
            LatLng position = item.getPosition();
            latSum += position.latitude;
            lngSum += position.longitude;
        }
        return new LatLng(latSum / items.size(), lngSum / items.size());
    }
}
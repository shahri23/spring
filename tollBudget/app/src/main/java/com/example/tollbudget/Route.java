package com.example.tollbudget;

import com.google.android.gms.maps.model.LatLng;
import java.util.List;

// Route model class
public class Route {
    private String routeName;
    private double tollCost;
    private int durationMinutes;
    private double distanceKm;
    private List<LatLng> polylinePoints;
    private LatLng destination;
    private List<TollSegment> tollSegments;
    private boolean isWithinBudget;

    public Route(String routeName, double tollCost, int durationMinutes, 
                 double distanceKm, List<LatLng> polylinePoints, LatLng destination) {
        this.routeName = routeName;
        this.tollCost = tollCost;
        this.durationMinutes = durationMinutes;
        this.distanceKm = distanceKm;
        this.polylinePoints = polylinePoints;
        this.destination = destination;
        this.tollSegments = new ArrayList<>();
    }

    // Getters and setters
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public double getTollCost() { return tollCost; }
    public void setTollCost(double tollCost) { this.tollCost = tollCost; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public List<LatLng> getPolylinePoints() { return polylinePoints; }
    public void setPolylinePoints(List<LatLng> polylinePoints) { this.polylinePoints = polylinePoints; }

    public LatLng getDestination() { return destination; }
    public void setDestination(LatLng destination) { this.destination = destination; }

    public List<TollSegment> getTollSegments() { return tollSegments; }
    public void setTollSegments(List<TollSegment> tollSegments) { this.tollSegments = tollSegments; }

    public boolean isWithinBudget() { return isWithinBudget; }
    public void setWithinBudget(boolean withinBudget) { isWithinBudget = withinBudget; }

    public String getFormattedDuration() {
        int hours = durationMinutes / 60;
        int mins = durationMinutes % 60;
        if (hours > 0) {
            return String.format("%dh %dm", hours, mins);
        } else {
            return String.format("%dm", mins);
        }
    }

    public String getFormattedDistance() {
        return String.format("%.1f km", distanceKm);
    }

    public String getFormattedCost() {
        if (tollCost == 0) {
            return "Free";
        }
        return String.format("$%.2f", tollCost);
    }
}

// Toll segment class for detailed toll information
class TollSegment {
    private String segmentName;
    private double cost;
    private LatLng startPoint;
    private LatLng endPoint;

    public TollSegment(String segmentName, double cost, LatLng startPoint, LatLng endPoint) {
        this.segmentName = segmentName;
        this.cost = cost;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    public String getSegmentName() { return segmentName; }
    public void setSegmentName(String segmentName) { this.segmentName = segmentName; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public LatLng getStartPoint() { return startPoint; }
    public void setStartPoint(LatLng startPoint) { this.startPoint = startPoint; }

    public LatLng getEndPoint() { return endPoint; }
    public void setEndPoint(LatLng endPoint) { this.endPoint = endPoint; }
}

// Interface for route service callbacks
interface RouteCallback {
    void onRoutesFound(List<Route> routes);
    void onError(String error);
}

// Interface for route selection
interface RouteSelectionListener {
    void onRouteSelected(Route route);
}

// Route service class for API calls
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Arrays;

class RouteService {
    
    public static void findRoutes(LatLng origin, String destination, double budget, RouteCallback callback) {
        // Simulate API call with background thread
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate network delay
                
                // Mock route data - replace with actual API calls
                List<Route> routes = generateMockRoutes(origin, destination, budget);
                
                // Filter routes by budget
                List<Route> budgetRoutes = new ArrayList<>();
                for (Route route : routes) {
                    if (route.getTollCost() <= budget) {
                        route.setWithinBudget(true);
                        budgetRoutes.add(route);
                    }
                }
                
                // Sort by duration (fastest first)
                budgetRoutes.sort((r1, r2) -> Integer.compare(r1.getDurationMinutes(), r2.getDurationMinutes()));
                
                // If no routes within budget, add cheapest option with warning
                if (budgetRoutes.isEmpty() && !routes.isEmpty()) {
                    Route cheapest = routes.stream()
                        .min((r1, r2) -> Double.compare(r1.getTollCost(), r2.getTollCost()))
                        .orElse(null);
                    if (cheapest != null) {
                        cheapest.setWithinBudget(false);
                        budgetRoutes.add(cheapest);
                    }
                }
                
                // Return results on main thread
                new Handler(Looper.getMainLooper()).post(() -> callback.onRoutesFound(budgetRoutes));
                
            } catch (InterruptedException e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Network error"));
            }
        }).start();
    }
    
    private static List<Route> generateMockRoutes(LatLng origin, String destination, double budget) {
        List<Route> routes = new ArrayList<>();
        
        // Mock destination coordinates (in real app, geocode the destination)
        LatLng dest = new LatLng(origin.latitude + 0.1, origin.longitude + 0.1);
        
        // Mock polyline points
        List<LatLng> polyline1 = Arrays.asList(
            origin,
            new LatLng(origin.latitude + 0.05, origin.longitude + 0.05),
            dest
        );
        
        List<LatLng> polyline2 = Arrays.asList(
            origin,
            new LatLng(origin.latitude + 0.03, origin.longitude + 0.08),
            dest
        );
        
        List<LatLng> polyline3 = Arrays.asList(
            origin,
            new LatLng(origin.latitude + 0.08, origin.longitude + 0.03),
            dest
        );
        
        // Route 1: Fastest with tolls
        Route route1 = new Route("Highway Express", 8.50, 45, 65.2, polyline1, dest);
        
        // Route 2: Medium speed, lower tolls
        Route route2 = new Route("Mixed Route", 3.25, 58, 68.4, polyline2, dest);
        
        // Route 3: Toll-free but slower
        Route route3 = new Route("Local Roads", 0.00, 72, 71.8, polyline3, dest);
        
        // Route 4: Expensive toll route
        Route route4 = new Route("Premium Highway", 15.75, 38, 62.1, polyline1, dest);
        
        routes.add(route1);
        routes.add(route2);
        routes.add(route3);
        routes.add(route4);
        
        return routes;
    }
}
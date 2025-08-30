package com.example.tollbudget;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {
    
    private List<Route> routes;
    private RouteSelectionListener listener;
    private int selectedPosition = -1;
    
    public RouteAdapter(List<Route> routes, RouteSelectionListener listener) {
        this.routes = routes;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Route route = routes.get(position);
        holder.bind(route, position);
    }
    
    @Override
    public int getItemCount() {
        return routes.size();
    }
    
    class RouteViewHolder extends RecyclerView.ViewHolder {
        
        private CardView cardView;
        private TextView tvRouteName;
        private TextView tvDuration;
        private TextView tvDistance;
        private TextView tvCost;
        private TextView tvBudgetStatus;
        private View statusIndicator;
        
        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_route);
            tvRouteName = itemView.findViewById(R.id.tv_route_name);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvCost = itemView.findViewById(R.id.tv_cost);
            tvBudgetStatus = itemView.findViewById(R.id.tv_budget_status);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }
        
        public void bind(Route route, int position) {
            tvRouteName.setText(route.getRouteName());
            tvDuration.setText(route.getFormattedDuration());
            tvDistance.setText(route.getFormattedDistance());
            tvCost.setText(route.getFormattedCost());
            
            // Set budget status and colors
            if (route.isWithinBudget()) {
                tvBudgetStatus.setText("Within Budget");
                tvBudgetStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark));
                statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_light));
                cardView.setCardBackgroundColor(Color.WHITE);
            } else {
                tvBudgetStatus.setText("Over Budget");
                tvBudgetStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark));
                statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_light));
                cardView.setCardBackgroundColor(Color.parseColor("#FFF3E0")); // Light orange background
            }
            
            // Highlight selected route
            if (selectedPosition == position) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_blue_light));
                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_blue_dark));
                cardView.setStrokeWidth(4);
            } else {
                cardView.setStrokeWidth(0);
            }
            
            // Set click listener
            cardView.setOnClickListener(v -> {
                int previousSelected = selectedPosition;
                selectedPosition = position;
                
                // Notify changes for animation
                if (previousSelected != -1) {
                    notifyItemChanged(previousSelected);
                }
                notifyItemChanged(selectedPosition);
                
                // Trigger callback
                if (listener != null) {
                    listener.onRouteSelected(route);
                }
            });
            
            // Add toll/free indicator
            if (route.getTollCost() == 0) {
                tvCost.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_free), 
                    null, null, null);
            } else {
                tvCost.setCompoundDrawablesWithIntrinsicBounds(
                    ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_toll), 
                    null, null, null);
            }
        }
    }
}
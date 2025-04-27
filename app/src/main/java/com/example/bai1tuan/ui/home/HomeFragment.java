package com.example.bai1tuan.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import com.example.bai1tuan.databinding.FragmentHomeBinding;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private EditText currencyToBeConverted;
    private EditText currencyConverted;
    private Spinner convertToDropdown;
    private Spinner convertFromDropdown;
    private Button convertButton;

    public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize views
        currencyConverted = binding.currencyConverted;
        currencyToBeConverted = binding.currencyToBeConverted;
        convertToDropdown = binding.convertTo;
        convertFromDropdown = binding.convertFrom;
        convertButton = binding.button;

        // Setup spinners with VND added
        String[] dropDownList = {"VND", "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "HKD", "NZD"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, dropDownList);
        convertToDropdown.setAdapter(adapter);
        convertFromDropdown.setAdapter(adapter);

        // Setup convert button click listener
        convertButton.setOnClickListener(v -> {
            if (currencyToBeConverted.getText().toString().isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }

            RetrofitInterface retrofitInterface = RetrofitBuilder.getRetrofitInstance()
                    .create(RetrofitInterface.class);
            
            Call<String> call = retrofitInterface.getExchangeCurrency(
                    Key.API_KEY,
                    convertFromDropdown.getSelectedItem().toString());
            
            call.enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            JSONObject jsonObject = new JSONObject(response.body());
                            JSONObject rates = jsonObject.getJSONObject("conversion_rates");
                            double currency = Double.parseDouble(currencyToBeConverted.getText().toString());
                            double multiplier = rates.getDouble(convertToDropdown.getSelectedItem().toString());
                            double result = currency * multiplier;
                            currencyConverted.setText(String.format("%,.0f", result)); // Format with thousands separator
                        } else {
                            Toast.makeText(requireContext(), "Không thể lấy tỷ giá", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(requireContext(), "Lỗi kết nối: " + t.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
package com.skr.rdn;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.StrictMode;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
	
	LinearLayout llMain;
	TextView textView, totalAmount;
	Button payBtn;

	String imgRootPath = "https://kcksejyyjfgpcdmgtzrc.supabase.co/storage/v1/object/public/product_images/";
	String url = "https://digitalrdn.netlify.app/.netlify/functions/get-data";

	double total = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.setTitle("Rongpur Daily Needs");
		setContentView(R.layout.activity_main);

		llMain = findViewById(R.id.llMain);
		textView = findViewById(R.id.textView);
		totalAmount = findViewById(R.id.totalAmount);
		payBtn = findViewById(R.id.payNow);

		payBtn.setOnClickListener(v -> payWithUPI());

		fetchAndRenderProduct();
	}

	private void fetchAndRenderProduct() {
		RequestQueue queue = Volley.newRequestQueue(this);
		JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
		Request.Method.GET, url, null,
		response -> {
			try {
				JSONArray products = response.getJSONArray("data");
				for (int i = 0; i < products.length(); i++) {
					JSONObject product = products.getJSONObject(i);
					addProduct(product);
				}
			} catch (JSONException e){
				Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
			}
		},
		error -> {
			Toast.makeText(this, "VolleyError: " + error.toString(), Toast.LENGTH_LONG).show();
		});
		queue.add(jsonObjectRequest);
	}

	private void addProduct(JSONObject p) {
		try {
			String name = p.getString("name");
			String price = p.getString("price");
			String unit = p.getString("unit");
			String type = p.getString("type");
			String imgUrl = imgRootPath + p.getString("file_name");

			// llMain is your parent layout (should be vertical)
			LinearLayout llProduct = new LinearLayout(this);
			llProduct.setOrientation(LinearLayout.HORIZONTAL);
			llProduct.setPadding(16, 16, 16, 16);
			llProduct.setElevation(16f);
			llProduct.setGravity(Gravity.CENTER_VERTICAL);
			llProduct.setBackgroundResource(R.drawable.card_bg); // set background drawable

			// Create LayoutParams with margin
			LinearLayout.LayoutParams productParams = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT
			);

			// Set margin in pixels (left, top, right, bottom)
			productParams.setMargins(16, 16, 16, 16); // 16px
			llProduct.setLayoutParams(productParams);

			// Image
			ImageView img = new ImageView(this);
			Glide.with(this)
			.load(imgUrl)
			//.override(200, 200) // Optional: same size as your layout
			//.centerCrop()	   // Optional: keeps aspect ratio
			.into(img);
			LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(200, 200);
			imgParams.setMargins(0, 0, 24, 0);
			img.setLayoutParams(imgParams);

			// Info Layout
			LinearLayout llInfo = new LinearLayout(this);
			llInfo.setOrientation(LinearLayout.VERTICAL);
			llInfo.setLayoutParams(new LinearLayout.LayoutParams(
			0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f // take remaining space
			));

			// Name
			TextView txtName = new TextView(this);
			txtName.setText(name);
			txtName.setTextSize(18);
			//txtName.setTypeface(null, Typeface.BOLD);

			// Price
			TextView txtPrice = new TextView(this);
			txtPrice.setText("Rate: ₹" + price + "/" + unit);

			// Quantity input
			EditText etQuantity = new EditText(this);
			etQuantity.setInputType(InputType.TYPE_CLASS_NUMBER);
			etQuantity.setText("1");
			etQuantity.setEms(3);

			// Add views to Info
			llInfo.addView(txtName);
			llInfo.addView(txtPrice);
			llInfo.addView(etQuantity);

			// Button
			Button btnAdd = new Button(this);
			btnAdd.setBackgroundResource(R.drawable.rounded_button);
			btnAdd.setText("Add");
			btnAdd.setBackgroundColor(Color.parseColor("#0f3d2e"));
			btnAdd.setTextColor(Color.WHITE);
			btnAdd.setPadding(24, 16, 24, 16);
			btnAdd.setOnClickListener(v -> {
				if (btnAdd.getText() == "Add") {
					updateTotalAmount(Double.parseDouble(price) * Double.parseDouble(etQuantity.getText().toString()));
					btnAdd.setBackgroundColor(Color.parseColor("#aa3040"));
					btnAdd.setText("Remove");
				}
				else {
					updateTotalAmount(Double.parseDouble("-" + price) * Double.parseDouble(etQuantity.getText().toString()));
					btnAdd.setBackgroundColor(Color.parseColor("#0f3d2e"));
					btnAdd.setText("Add");
				}
			});

			// Add to main product layout
			llProduct.addView(img);
			llProduct.addView(llInfo);
			llProduct.addView(btnAdd);

			// Finally add to main layout
			llMain.addView(llProduct);
		} catch (JSONException e){
			Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
		}
	}

	private void updateTotalAmount(Double productPrice) {
		//Toast.makeText(this, String.format(Locale.US, "%.2f", total), Toast.LENGTH_SHORT).show();
		total += productPrice;
		totalAmount.setText(String.format(Locale.US, "%.2f", total));
	}

	private void payWithUPI() {
		if (total < 10) {
			Toast.makeText(this, "Minimum purchase Rs. 10", Toast.LENGTH_SHORT).show();
			return;
		}

		String uri = String.format(Locale.US, "upi://pay?pa=Q060474773@ybl&pn=Rongpur Daily Needs&am=%.2f&cu=INR", total);

		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

		try {
			startActivity(intent);
		} catch (Exception e) {
			Toast.makeText(this, "No UPI app found", Toast.LENGTH_SHORT).show();
		}
	}
}

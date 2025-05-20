package com.example.cfeprjct.Activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cfeprjct.Adapters.CatalogItem;
import com.example.cfeprjct.AppDatabase;
import com.example.cfeprjct.AuthUtils;
import com.example.cfeprjct.DAOS.OrderDAO;
import com.example.cfeprjct.DAOS.OrderedDessertDAO;
import com.example.cfeprjct.DAOS.OrderedDishDAO;
import com.example.cfeprjct.DAOS.OrderedDrinkDAO;
import com.example.cfeprjct.Entities.CartItem;
import com.example.cfeprjct.Entities.Review;
import com.example.cfeprjct.Entities.Volume;
import com.example.cfeprjct.R;
import com.example.cfeprjct.Sync.CatalogSync;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM = "extra_catalog_item";

    private AppDatabase db;
    private FirebaseFirestore firestore;

    // базовая цена и вычисленная (с надбавкой для M/L)
    private float basePrice;
    private float calculatedPrice;
    private int selectedVolumeMl;

    private TextView tvPrice, tvRating, tvSize, tvLabelSize;
    private MaterialButtonToggleGroup sizes;
    private int mlS, mlM, mlL;

    // отзывы
    private View reviewSection;
    private RatingBar ratingBar;
    private EditText etComment;
    private MaterialButton btnSubmitReview;
    private RecyclerView rvReviews;
    private ListAdapter<Review, ?> reviewAdapter;

    private String userId;
    private boolean isLoggedIn;
    private String productType;
    private int productId;

    // DAO для заказов (только на проверку покупки, отправка в корзину не тут)
    private OrderDAO orderDAO;
    private OrderedDrinkDAO orderedDrinkDAO;
    private OrderedDishDAO orderedDishDAO;
    private OrderedDessertDAO orderedDessertDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // edge‐to‐edge padding для тулбара
        View root = findViewById(R.id.coordinator_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            MaterialToolbar tb = findViewById(R.id.toolbar);
            tb.setPadding(tb.getPaddingLeft(), sb.top,
                    tb.getPaddingRight(), tb.getPaddingBottom());
            return insets;
        });

        // получаем переданный CatalogItem
        CatalogItem item = (CatalogItem) getIntent().getSerializableExtra(EXTRA_ITEM);
        if (item == null) {
            finish();
            return;
        }
        productType = item.getType();
        productId   = item.getId();

        // настраиваем тулбар
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // findViewById
        ImageView iv       = findViewById(R.id.detailImage);
        TextView tvTitle   = findViewById(R.id.detailTitle);
        tvRating           = findViewById(R.id.detailRating);
        TextView tvDesc    = findViewById(R.id.detailDescription);
        tvSize             = findViewById(R.id.detailSize);
        tvLabelSize        = findViewById(R.id.labelSize);
        sizes              = findViewById(R.id.sizeToggle);
        MaterialButton btnAdd = findViewById(R.id.btnAddToCart);
        tvPrice            = findViewById(R.id.detailPrice);

        reviewSection      = findViewById(R.id.reviewSection);
        ratingBar          = findViewById(R.id.ratingBar);
        etComment          = findViewById(R.id.etComment);
        btnSubmitReview    = findViewById(R.id.btnSubmitReview);
        rvReviews          = findViewById(R.id.rvReviews);

        // настроим список отзывов
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter();
        rvReviews.setAdapter((RecyclerView.Adapter<?>) reviewAdapter);

        // заполняем UI
        tvTitle.setText(item.getTitle());
        tvRating.setText(String.format(Locale.getDefault(), "%.1f", item.getRating()));
        tvDesc.setText(item.getDescription());
        Glide.with(this)
                .load(item.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .centerCrop()
                .into(iv);

        // для блюд/десертов показываем вес, скрываем toggle
        if ("dish".equals(productType) || "dessert".equals(productType)) {
            tvLabelSize.setVisibility(View.GONE);
            sizes.setVisibility(View.GONE);
            int sz = item.getSize();
            if (sz > 0) {
                tvSize.setVisibility(View.VISIBLE);
                tvSize.setText("Вес: " + sz + " г");
            } else {
                tvSize.setVisibility(View.GONE);
            }
        } else {
            // напиток: показываем toggle, скрываем текстовое поле
            tvSize.setVisibility(View.GONE);
            tvLabelSize.setVisibility(View.VISIBLE);
            sizes.setVisibility(View.VISIBLE);
        }

        // инициализируем цены
        basePrice      = item.getPrice();
        calculatedPrice = basePrice;
        tvPrice.setText(String.format("%d ₽", (int) calculatedPrice));

        // БД и Firestore
        db        = AppDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();

        orderDAO          = db.orderDAO();
        orderedDrinkDAO   = db.orderedDrinkDAO();
        orderedDishDAO    = db.orderedDishDAO();
        orderedDessertDAO = db.orderedDessertDAO();

        isLoggedIn = AuthUtils.isLoggedIn(this);
        userId     = AuthUtils.getLoggedInUserId(this);

        // лайвдата отзывов
        LiveData<List<Review>> liveReviews;
        switch (productType) {
            case "drink":
                liveReviews = db.reviewDAO().getReviewsForDrinkId(productId);
                break;
            case "dish":
                liveReviews = db.reviewDAO().getReviewsForDishId(productId);
                break;
            default:
                liveReviews = db.reviewDAO().getReviewsForDessertId(productId);
                break;
        }
        liveReviews.observe(this, reviews -> {
            if (reviews == null || reviews.isEmpty()) {
                reviewSection.setVisibility(View.GONE);
            } else {
                reviewSection.setVisibility(View.VISIBLE);
                reviewAdapter.submitList(reviews);
                float sum = 0f;
                for (Review r : reviews) sum += r.getRating();
                tvRating.setText(String.format(Locale.getDefault(), "%.1f", sum / reviews.size()));
            }
        });

        // загрузка объёмов для напитков
        if ("drink".equals(productType)) {
            new CatalogSync(this).syncVolumes(() -> {
                List<Volume> vols = db.volumeDAO().getAllVolumes();
                for (Volume v : vols) {
                    switch (v.getSize()) {
                        case "S": mlS = v.getMl(); break;
                        case "M": mlM = v.getMl(); break;
                        case "L": mlL = v.getMl(); break;
                    }
                }
                runOnUiThread(this::configureSizeToggle);
            });
        } else {
            sizes.setVisibility(View.GONE);
        }

        // добавление в корзину
        btnAdd.setOnClickListener(v -> {
            new Thread(() -> {
                int sizeValue = "drink".equals(productType)
                        ? selectedVolumeMl
                        : item.getSize();

                CartItem existing = db.cartItemDao()
                        .getByProductAndSize(productType, productId, sizeValue);
                if (existing != null) {
                    int q = Math.min(existing.getQuantity() + 1, 15);
                    existing.setQuantity(q);
                    db.cartItemDao().update(existing);
                } else {
                    CartItem ci = new CartItem();
                    ci.setProductType(productType);
                    ci.setProductId(productId);
                    ci.setTitle(item.getTitle());
                    ci.setImageUrl(item.getImageUrl());
                    ci.setSize(sizeValue);
                    // вот тут важно: используем calculatedPrice, а не 0
                    ci.setUnitPrice(calculatedPrice);
                    ci.setQuantity(1);
                    db.cartItemDao().insert(ci);

                    Map<String,Object> map = new HashMap<>();
                    map.put("id",          ci.getId());
                    map.put("userId",      userId);
                    map.put("productType", ci.getProductType());
                    map.put("productId",   ci.getProductId());
                    map.put("title",       ci.getTitle());
                    map.put("imageUrl",    ci.getImageUrl());
                    map.put("size",        ci.getSize());
                    map.put("unitPrice",   ci.getUnitPrice());
                    map.put("quantity",    ci.getQuantity());

                    firestore.collection("carts")
                            .document(userId)
                            .collection("items")
                            .document(String.valueOf(ci.getId()))
                            .set(map);
                }
                runOnUiThread(() ->
                        Toast.makeText(this, "Добавлено в корзину", Toast.LENGTH_SHORT).show()
                );
            }).start();
        });
    }

    private void configureSizeToggle() {
        // по умолчанию — S
        sizes.check(R.id.btnSizeS);
        selectedVolumeMl = mlS;
        calculatedPrice  = basePrice;
        tvPrice.setText(String.format("%d ₽", (int) calculatedPrice));

        sizes.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnSizeM) {
                selectedVolumeMl = mlM;
                calculatedPrice  = basePrice + 100;
            } else if (checkedId == R.id.btnSizeL) {
                selectedVolumeMl = mlL;
                calculatedPrice  = basePrice + 150;
            } else {
                selectedVolumeMl = mlS;
                calculatedPrice  = basePrice;
            }
            tvPrice.setText(String.format("%d ₽", (int) calculatedPrice));
        });
    }

    private class ReviewAdapter extends ListAdapter<Review, ReviewAdapter.VH> {
        ReviewAdapter() {
            super(new DiffUtil.ItemCallback<Review>() {
                @Override public boolean areItemsTheSame(Review a, Review b) {
                    return a.getReviewId() == b.getReviewId();
                }
                @Override public boolean areContentsTheSame(Review a, Review b) {
                    return a.getRating() == b.getRating()
                            && a.getText().equals(b.getText())
                            && a.getReviewDate() == b.getReviewDate();
                }
            });
        }
        @Override public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_review, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            Review r = getItem(pos);
            h.ratingBar.setRating(r.getRating());
            h.tvText.setText(r.getText());
            h.tvUserName.setText("Аноним");
            String reviewerId = r.getUserId();
            if (reviewerId != null && !reviewerId.isEmpty()) {
                firestore.collection("users")
                        .document(reviewerId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String first = doc.getString("firstName");
                                String last  = doc.getString("lastName");
                                String full  = ((first!=null)?first:"")
                                        + ((last!=null)?" "+last:"");
                                h.tvUserName.setText(full.trim().isEmpty()?"Аноним":full);
                            }
                        });
            }
        }
        class VH extends RecyclerView.ViewHolder {
            RatingBar ratingBar;
            TextView tvUserName, tvText;
            VH(View v) {
                super(v);
                ratingBar  = v.findViewById(R.id.itemRatingBar);
                tvUserName = v.findViewById(R.id.itemUserName);
                tvText     = v.findViewById(R.id.itemReviewText);
            }
        }
    }
}

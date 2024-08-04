package com.example.registerpage;

import android.os.Bundle;
        import android.view.View;
        import android.widget.ImageView;
        import androidx.appcompat.app.AppCompatActivity;
        import androidx.cardview.widget.CardView;

public class InformationActivity extends AppCompatActivity {

    private CardView[] diseaseCards = new CardView[6];
    private View[] diseaseDetails = new View[6];
    private ImageView[] diseaseImages = new ImageView[6];
    private int[][] diseaseImageResources = new int[6][];
    private int[] currentImageIndices = new int[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        initializeViews();
        setUpClickListeners();
        initializeImageResources();
    }

    private void initializeViews() {
        int[] cardIds = {R.id.earlyBlightCard, R.id.lateBlightCard, R.id.leafMoldCard,
                R.id.septoriaLeafSpotCard, R.id.bacterialSpotCard, R.id.spiderMitesCard};
        int[] detailIds = {R.id.earlyBlightDetails, R.id.lateBlightDetails, R.id.leafMoldDetails,
                R.id.septoriaLeafSpotDetails, R.id.bacterialSpotDetails, R.id.spiderMitesDetails};
        int[] imageIds = {R.id.earlyBlightImage, R.id.lateBlightImage, R.id.leafMoldImage,
                R.id.septoriaLeafSpotImage, R.id.bacterialSpotImage, R.id.spiderMitesImage};

        for (int i = 0; i < 6; i++) {
            diseaseCards[i] = findViewById(cardIds[i]);
            diseaseDetails[i] = findViewById(detailIds[i]);
            diseaseImages[i] = findViewById(imageIds[i]);
        }
    }

    private void setUpClickListeners() {
        for (int i = 0; i < 6; i++) {
            final int index = i;
            diseaseCards[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleDetails(index);
                }
            });
        }
    }

    private void initializeImageResources() {
        diseaseImageResources[0] = new int[]{R.drawable.early_blight1, R.drawable.early_blight2, R.drawable.early_blight3};
        diseaseImageResources[1] = new int[]{R.drawable.late_blight1, R.drawable.late_blight2, R.drawable.late_blight3};
        diseaseImageResources[2] = new int[]{R.drawable.leaf_mold1, R.drawable.leaf_mold2, R.drawable.leaf_mold3};
        diseaseImageResources[3] = new int[]{R.drawable.septoria_leaf_spot1, R.drawable.sep2, R.drawable.septoria_leaf_spot1};
        diseaseImageResources[4] = new int[]{R.drawable.bacteria_spot1, R.drawable.bac3, R.drawable.bacteria_spot4};
        diseaseImageResources[5] = new int[]{R.drawable.smt1, R.drawable.smt2, R.drawable.smt3};

    }

    private void toggleDetails(int index) {
        if (diseaseDetails[index].getVisibility() == View.VISIBLE) {
            diseaseDetails[index].setVisibility(View.GONE);
        } else {
            diseaseDetails[index].setVisibility(View.VISIBLE);
            startImageRotation(index);
        }
    }

    private void startImageRotation(final int index) {
        diseaseImages[index].postDelayed(new Runnable() {
            @Override
            public void run() {
                currentImageIndices[index] = (currentImageIndices[index] + 1) % diseaseImageResources[index].length;
                diseaseImages[index].setImageResource(diseaseImageResources[index][currentImageIndices[index]]);
                if (diseaseDetails[index].getVisibility() == View.VISIBLE) {
                    startImageRotation(index);
                }
            }
        }, 2000);
    }
}
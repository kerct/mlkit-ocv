package com.example.mlkitocv;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NameActivity extends AppCompatActivity {
    private EditText name;
    private Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name);

        name = findViewById(R.id.name);
        next = findViewById(R.id.nextButton);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(name.getText().toString().equals("")) {
                    Toast.makeText(NameActivity.this, "Please enter a name", Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(NameActivity.this, name.getText().toString(), Toast.LENGTH_LONG).show();
                    //Intent intent = new Intent(NameActivity.this, Training.class);
                    //intent.putExtra("name", name.getText().toString().trim());
                    //startActivity(intent);
                }
            }
        });
    }
}

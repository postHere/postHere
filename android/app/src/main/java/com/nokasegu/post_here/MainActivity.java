package com.nokasegu.post_here;

import android.os.Build;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // [최종 해결책] Activity 시작 시 Android 시스템에 상태 표시줄 스타일을 강제 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Activity가 시스템 UI(상태 표시줄)와 상호작용할 수 있도록 설정
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

            // 1. 상태 표시줄 배경을 투명하게 설정합니다. (styles.xml과 일치)
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

            // 2. [핵심 수정] WindowInsetsControllerCompat을 사용하여 아이콘 스타일을 강제합니다.
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

            if (windowInsetsController != null) {
                // Style.Dark에 해당: 상태 표시줄 아이콘을 어둡게 설정합니다.
                windowInsetsController.setAppearanceLightStatusBars(true);
            }
        }
    }
}

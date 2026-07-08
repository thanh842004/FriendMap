package com.example.friendmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity_GPS";
    GoogleMap mMap;
    FusedLocationProviderClient fusedLocationClient;
    LocationCallback locationCallback;
    DatabaseReference locationRef;
    FirebaseAuth mAuth;
    BottomNavigationView bottomNav;
    FirebaseFirestore firestore;

    FloatingActionButton fabDirections;
    LatLng selectedFriendLatLng = null; // Lưu tọa độ của người bạn vừa click chọn

    HashMap<String, Marker> friendMarkers = new HashMap<>();
    List<String> friendIds = new ArrayList<>();
    HashMap<String, String> friendNamesCache = new HashMap<>();

    private ListenerRegistration firestoreListener;
    private List<ValueEventListener> realtimeListeners = new ArrayList<>();
    private List<DatabaseReference> listenedRefs = new ArrayList<>();

    static final int LOCATION_PERMISSION_REQUEST = 1001;
    static final int NOTIFICATION_PERMISSION_REQUEST = 1002; // Mã yêu cầu quyền thông báo
    private boolean isFirstZoom = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // FIX LỖI CỐT LÕI: Ép đúng cụm máy chủ Singapore tránh bị rớt luồng mạng
        locationRef = FirebaseDatabase.getInstance("https://friendmap-53fe9-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("locations");
        firestore = FirebaseFirestore.getInstance();

        fabDirections = findViewById(R.id.fabDirections);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.fabMyLocation).setOnClickListener(v -> moveToMyLocation());

        // LOGIC CHỈ ĐƯỜNG: Gọi Intent mở Google Maps điều hướng thông minh bằng tọa độ thực tế
        if (fabDirections != null) {
            fabDirections.setOnClickListener(v -> {
                if (selectedFriendLatLng != null) {
                    String mapUri = "google.navigation:q=" + selectedFriendLatLng.latitude + "," + selectedFriendLatLng.longitude + "&mode=d";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUri));
                    intent.setPackage("com.google.android.apps.maps");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        String webUri = "https://www.google.com/maps/dir/?api=1&destination=" + selectedFriendLatLng.latitude + "," + selectedFriendLatLng.longitude;
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
                    }
                }
            });
        }

        bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_map);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_map) {
                return true;
            }

            Intent intent = null;
            if (id == R.id.nav_friends) {
                intent = new Intent(this, FriendsActivity.class);
            } else if (id == R.id.nav_chat) {
                intent = new Intent(this, ChatActivity.class);
            } else if (id == R.id.nav_profile) {
                intent = new Intent(this, ProfileActivity.class);
            }

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // ĐỒNG BỘ THÔNG BÁO CHÍ MẠNG: Xin quyền hiển thị thông báo khẩn cấp cho Android 13 trở lên (API 33+)
        checkAndRequestNotificationPermission();
    }

    // Hàm tự động kiểm tra và kích hoạt hộp thoại xin quyền thông báo hệ thống
    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        } catch (Exception e) {
            Log.e(TAG, "Lỗi nạp style bản đồ: " + e.getMessage());
        }

        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setOnMarkerClickListener(marker -> {
            selectedFriendLatLng = marker.getPosition();
            if (fabDirections != null) {
                fabDirections.setVisibility(View.VISIBLE);
            }
            marker.showInfoWindow();
            return false;
        });

        mMap.setOnMapClickListener(latLng -> {
            if (fabDirections != null) {
                fabDirections.setVisibility(View.GONE);
                selectedFriendLatLng = null;
            }
        });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }

        loadFriendsAndShowOnMap();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateLocationToFirebase(location);

                    if (isFirstZoom) {
                        LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15f));
                        isFirstZoom = false;
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void updateLocationToFirebase(Location location) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", System.currentTimeMillis());
        locationData.put("isOnline", true);

        locationRef.child(uid).setValue(locationData)
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi đẩy định vị lên Realtime: " + e.getMessage()));
    }

    private void moveToMyLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 16f));
            }
        });
    }

    private void clearRealtimeListenersAnToan() {
        if (realtimeListeners != null && listenedRefs != null) {
            int listenerCount = Math.min(realtimeListeners.size(), listenedRefs.size());
            for (int i = listenerCount - 1; i >= 0; i--) {
                try {
                    if (listenedRefs.get(i) != null && realtimeListeners.get(i) != null) {
                        listenedRefs.get(i).removeEventListener(realtimeListeners.get(i));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi giải phóng tại vị trí: " + i);
                }
            }
            realtimeListeners.clear();
            listenedRefs.clear();
        }
    }

    private void loadFriendsAndShowOnMap() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUid = mAuth.getCurrentUser().getUid();

        firestoreListener = firestore.collection("friendRequests")
                .whereEqualTo("status", "accepted")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Lỗi lắng nghe dữ liệu bạn bè từ Firestore: ", error);
                        return;
                    }
                    if (value == null) return;

                    clearRealtimeListenersAnToan();
                    friendIds.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        String senderId = doc.getString("senderId");
                        String receiverId = doc.getString("receiverId");

                        if (currentUid.equals(senderId) && receiverId != null) {
                            friendIds.add(receiverId);
                        } else if (currentUid.equals(receiverId) && senderId != null) {
                            friendIds.add(senderId);
                        }
                    }

                    Log.d(TAG, "Tìm thấy " + friendIds.size() + " người bạn đã liên kết thành công.");

                    for (String friendId : friendIds) {
                        DatabaseReference friendLocationRef = locationRef.child(friendId);

                        ValueEventListener vListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (!snapshot.exists()) return;

                                Object latObj = snapshot.child("latitude").getValue();
                                Object lngObj = snapshot.child("longitude").getValue();
                                if (latObj == null || lngObj == null) return;

                                Double lat = Double.parseDouble(latObj.toString());
                                Double lng = Double.parseDouble(lngObj.toString());
                                LatLng friendLatLng = new LatLng(lat, lng);

                                if (friendNamesCache.containsKey(friendId)) {
                                    String name = friendNamesCache.get(friendId);
                                    updateMarkerOnMap(friendId, friendLatLng, name);
                                } else {
                                    firestore.collection("users").document(friendId).get()
                                            .addOnSuccessListener(userDoc -> {
                                                String finalName = "Bạn bè";
                                                if (userDoc.exists()) {
                                                    String hoTen = userDoc.getString("hoTen");
                                                    String displayName = userDoc.getString("displayName");
                                                    String username = userDoc.getString("username");

                                                    if (hoTen != null && !hoTen.isEmpty()) finalName = hoTen;
                                                    else if (displayName != null && !displayName.isEmpty()) finalName = displayName;
                                                    else if (username != null && !username.isEmpty()) finalName = username;
                                                }
                                                friendNamesCache.put(friendId, finalName);
                                                updateMarkerOnMap(friendId, friendLatLng, finalName);
                                            })
                                            .addOnFailureListener(e -> updateMarkerOnMap(friendId, friendLatLng, "Bạn bè"));
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "Lỗi kết nối Realtime DB: " + error.getMessage());
                            }
                        };

                        friendLocationRef.addValueEventListener(vListener);
                        listenedRefs.add(friendLocationRef);
                        realtimeListeners.add(vListener);
                    }
                });
    }

    private void updateMarkerOnMap(String friendId, LatLng latLng, String title) {
        if (mMap == null) return;

        runOnUiThread(() -> {
            if (friendMarkers.containsKey(friendId)) {
                Marker marker = friendMarkers.get(friendId);
                if (marker != null) {
                    marker.setPosition(latLng);
                    marker.setTitle(title);
                    if (marker.isInfoWindowShown()) {
                        marker.showInfoWindow();
                    }
                }
            } else {
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(title));
                if (marker != null) {
                    friendMarkers.put(friendId, marker);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                }
            }
        });
    }

    // Quản lý phản hồi cấp quyền của người dùng
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Cần quyền vị trí để hiển thị bản đồ!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Người dùng đã cấp quyền thông báo đẩy thành công.");
            } else {
                Toast.makeText(this, "Bạn sẽ không nhận được thông báo tin nhắn mới khi tắt ứng dụng!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mAuth.getCurrentUser() != null && locationRef != null) {
            String uid = mAuth.getCurrentUser().getUid();
            locationRef.child(uid).child("isOnline").setValue(false);
        }

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (firestoreListener != null) {
            firestoreListener.remove();
        }

        clearRealtimeListenersAnToan();
        super.onDestroy();
    }
}
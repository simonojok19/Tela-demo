package co.planetsystems.tela.enroll;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import co.planetsystems.tela.R;

public class SupplementaryFragment extends Fragment {
    private static final String SCHOOL_NAME = "co.planetsystems.tela.enroll.SupplementaryFragment.SCHOOL_NAME";
    private static final String DISTRICT = "co.planetsystems.tela.enroll.SupplementaryFragment.DISTRICT";
    private static final String ROLE = "co.planetsystems.tela.enroll.SupplementaryFragment.ROLE";

    private EditText school;
    private EditText district;
    private EditText role;
    private Button previous;

    public SupplementaryFragment() {
        // Required empty public constructor
    }

    public static SupplementaryFragment newInstance(String schoolName, String district, String role) {
        SupplementaryFragment fragment = new SupplementaryFragment();
        Bundle args = new Bundle();
        args.putString(SCHOOL_NAME, schoolName);
        args.putString(DISTRICT, district);
        args.putString(ROLE, role);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_supplementary, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        school = view.findViewById(R.id.sup_school);
        district = view.findViewById(R.id.sup_district);
        role = view.findViewById(R.id.sup_role);
        previous = view.findViewById(R.id.sup_previous);



    }
}

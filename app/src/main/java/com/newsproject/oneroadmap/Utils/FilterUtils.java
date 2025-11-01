package com.newsproject.oneroadmap.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterUtils {
    private static final List<String> educationOptions = Arrays.asList(
            "Select Education Category",
            "Engineering", "Medical", "Dental", "Pharmacy", "Nursing", "Paramedical",
            "Biotechnology", "Agriculture", "Veterinary", "Law", "Management", "Commerce",
            "Arts", "Pure Science", "Computer Science/IT", "Architecture", "Hotel Management",
            "Design", "Journalism", "Education", "Polytechnic", "ITI", "Home Science",
            "Performing Arts", "Visual Arts", "Animation & Multimedia", "Aviation", "Social Work", "Other"
    );

    private static final Map<String, List<String>> degreeMap = new HashMap<>();
    private static final Map<String, List<String>> postGradMap = new HashMap<>();

    static {
        // Degree Map
        degreeMap.put("Engineering", Arrays.asList("Select Degree", "B.Tech", "B.E", "B.Sc Engineering", "Diploma in Engineering", "Other"));
        degreeMap.put("Medical", Arrays.asList("Select Degree", "MBBS", "BAMS", "BHMS", "BUMS", "Other"));
        degreeMap.put("Dental", Arrays.asList("Select Degree", "BDS", "Other"));
        degreeMap.put("Pharmacy", Arrays.asList("Select Degree", "B.Pharm", "D.Pharm", "Other"));
        degreeMap.put("Nursing", Arrays.asList("Select Degree", "B.Sc Nursing", "GNM", "ANM", "Other"));
        degreeMap.put("Paramedical", Arrays.asList("Select Degree", "BPT", "B.Sc MLT", "B.Sc Radiology", "Other"));
        degreeMap.put("Biotechnology", Arrays.asList("Select Degree", "B.Sc Biotechnology", "B.Tech Biotechnology", "Other"));
        degreeMap.put("Agriculture", Arrays.asList("Select Degree", "B.Sc Agriculture", "B.Tech Agriculture", "Other"));
        degreeMap.put("Veterinary", Arrays.asList("Select Degree", "BVSc", "Other"));
        degreeMap.put("Law", Arrays.asList("Select Degree", "LLB", "BA LLB", "BBA LLB", "Other"));
        degreeMap.put("Management", Arrays.asList("Select Degree", "BBA", "BMS", "Other"));
        degreeMap.put("Commerce", Arrays.asList("Select Degree", "B.Com", "B.Com (Hons)", "Other"));
        degreeMap.put("Arts", Arrays.asList("Select Degree", "BA", "BA (Hons)", "Other"));
        degreeMap.put("Pure Science", Arrays.asList("Select Degree", "B.Sc Physics", "B.Sc Chemistry", "B.Sc Maths", "B.Sc Biology", "Other"));
        degreeMap.put("Computer Science/IT", Arrays.asList("Select Degree", "BCA", "B.Sc IT", "B.Sc Computer Science", "Other"));
        degreeMap.put("Architecture", Arrays.asList("Select Degree", "B.Arch", "Other"));
        degreeMap.put("Hotel Management", Arrays.asList("Select Degree", "BHM", "B.Sc Hospitality", "Other"));
        degreeMap.put("Design", Arrays.asList("Select Degree", "B.Des Fashion", "B.Des Interior", "BFA", "Other"));
        degreeMap.put("Journalism", Arrays.asList("Select Degree", "BA Journalism", "B.Sc Mass Comm", "Other"));
        degreeMap.put("Education", Arrays.asList("Select Degree", "B.Ed", "BA B.Ed", "Other"));
        degreeMap.put("Polytechnic", Arrays.asList("Select Degree", "Diploma in Civil", "Diploma in Mechanical", "Diploma in Electrical", "Other"));
        degreeMap.put("ITI", Arrays.asList("Select Degree", "ITI in Electrician", "ITI in Fitter", "ITI in Welder", "Other"));
        degreeMap.put("Home Science", Arrays.asList("Select Degree", "B.Sc Home Science", "BA Home Science", "Other"));
        degreeMap.put("Performing Arts", Arrays.asList("Select Degree", "BPA Music", "BPA Dance", "BPA Theatre", "Other"));
        degreeMap.put("Visual Arts", Arrays.asList("Select Degree", "BFA Painting", "BFA Sculpture", "BFA Applied Arts", "Other"));
        degreeMap.put("Animation & Multimedia", Arrays.asList("Select Degree", "B.Sc Animation", "B.Des Animation", "Other"));
        degreeMap.put("Aviation", Arrays.asList("Select Degree", "B.Sc Aviation", "BBA Aviation", "Other"));
        degreeMap.put("Social Work", Arrays.asList("Select Degree", "BSW", "BA Social Work", "Other"));
        degreeMap.put("Other", Arrays.asList("Select Degree", "Other"));

        // Post Graduation Map
        postGradMap.put("Engineering", Arrays.asList("Select Post Graduation", "M.Tech", "M.E", "MBA", "None"));
        postGradMap.put("Medical", Arrays.asList("Select Post Graduation", "MD", "MS", "M.Sc", "None"));
        postGradMap.put("Dental", Arrays.asList("Select Post Graduation", "MDS", "None"));
        postGradMap.put("Pharmacy", Arrays.asList("Select Post Graduation", "M.Pharm", "None"));
        postGradMap.put("Nursing", Arrays.asList("Select Post Graduation", "M.Sc Nursing", "None"));
        postGradMap.put("Paramedical", Arrays.asList("Select Post Graduation", "MPT", "M.Sc", "None"));
        postGradMap.put("Biotechnology", Arrays.asList("Select Post Graduation", "M.Sc Biotechnology", "M.Tech", "None"));
        postGradMap.put("Agriculture", Arrays.asList("Select Post Graduation", "M.Sc Agriculture", "None"));
        postGradMap.put("Veterinary", Arrays.asList("Select Post Graduation", "MVSc", "None"));
        postGradMap.put("Law", Arrays.asList("Select Post Graduation", "LLM", "None"));
        postGradMap.put("Management", Arrays.asList("Select Post Graduation", "MBA", "PGDM", "None"));
        postGradMap.put("Commerce", Arrays.asList("Select Post Graduation", "M.Com", "MBA", "None"));
        postGradMap.put("Arts", Arrays.asList("Select Post Graduation", "MA", "None"));
        postGradMap.put("Pure Science", Arrays.asList("Select Post Graduation", "M.Sc", "None"));
        postGradMap.put("Computer Science/IT", Arrays.asList("Select Post Graduation", "MCA", "M.Sc IT", "None"));
        postGradMap.put("Architecture", Arrays.asList("Select Post Graduation", "M.Arch", "None"));
        postGradMap.put("Hotel Management", Arrays.asList("Select Post Graduation", "MHM", "MBA Hospitality", "None"));
        postGradMap.put("Design", Arrays.asList("Select Post Graduation", "M.Des", "None"));
        postGradMap.put("Journalism", Arrays.asList("Select Post Graduation", "MA Journalism", "None"));
        postGradMap.put("Education", Arrays.asList("Select Post Graduation", "M.Ed", "None"));
        postGradMap.put("Polytechnic", Arrays.asList("Select Post Graduation", "Advanced Diploma", "None"));
        postGradMap.put("ITI", Arrays.asList("Select Post Graduation", "None"));
        postGradMap.put("Home Science", Arrays.asList("Select Post Graduation", "M.Sc Home Science", "None"));
        postGradMap.put("Performing Arts", Arrays.asList("Select Post Graduation", "MPA", "None"));
        postGradMap.put("Visual Arts", Arrays.asList("Select Post Graduation", "MFA", "None"));
        postGradMap.put("Animation & Multimedia", Arrays.asList("Select Post Graduation", "M.Sc Animation", "None"));
        postGradMap.put("Aviation", Arrays.asList("Select Post Graduation", "M.Sc Aviation", "MBA Aviation", "None"));
        postGradMap.put("Social Work", Arrays.asList("Select Post Graduation", "MSW", "None"));
        postGradMap.put("Other", Arrays.asList("Select Post Graduation", "None"));
    }

    public static List<String> getEducationOptions() {
        return educationOptions;
    }

    public static Map<String, List<String>> getDegreeMap() {
        return degreeMap;
    }

    public static Map<String, List<String>> getPostGradMap() {
        return postGradMap;
    }
}
package com.newsproject.oneroadmap.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataConstants {
    public static final List<String> DISTRICTS = Arrays.asList( "Ahmednagar", "Akola", "Amravati", "Aurangabad", "Beed",
            "Bhandara", "Buldhana", "Chandrapur", "Dhule", "Gadchiroli", "Gondia",
            "Hingoli", "Jalgaon", "Jalna", "Kolhapur", "Latur", "Mumbai City",
            "Mumbai Suburban", "Nagpur", "Nanded", "Nandurbar", "Nashik", "Osmanabad",
            "Palghar", "Parbhani", "Pune", "Raigad", "Ratnagiri", "Sangli",
            "Satara", "Sindhudurg", "Solapur", "Thane", "Wardha", "Washim", "Yavatmal"
    );

    public static final Map<String, List<String>> TALUKA_MAP = new HashMap<>();

    public static final List<String> TWELFTH_OPTIONS = Arrays.asList(
             "सध्या दहावीला आहे", "सध्या बारावीला आह", "नाही या पुढील शिक्षण आहे ", "दहावी बारावी नंतर शाळा सोडली"
    );

    /** NEW – text that must appear in the two TextViews */
    public static final List<String> JOB_TEXT_BY_TWELFTH = Arrays.asList(
            "दहावी,बारावी आधारित जॉब पाहिजेत ?",          // default (index 0)
            "बारावी व डीग्री आधारित जॉब पाहिजेत ?",      // index 1
            "दहावी व डीग्री आधारित जॉब पाहिजेत ?",      // index 2
            "दहावी व बारावी आधारित जॉब पाहिजेत ?",      // index 3
            "डीग्री आधारित जॉब पाहिजेत ?"               // index 4
    );


    public static final List<String> EDUCATION_OPTIONS = Arrays.asList(
            
            "Education", "Arts", "Commerce", "Engineering", "Diploma",
            "Medical", "Dental", "ITI", "Pharmacy", "Agriculture",
            "Computer Science/IT", "Nursing", "Law", "Veterinary",
            "Journalism", "Management", "Hotel Management",
            "Animation & Multimedia", "Other B.Sc", "Other"
    );


    public static final Map<String, List<String>> DEGREE_MAP = new HashMap<>();
    public static final Map<String, List<String>> POST_GRAD_MAP = new HashMap<>();

    static {
        // Initialize TALUKA_MAP (same as in LoginPage3)
        TALUKA_MAP.put("Ahmednagar", Arrays.asList( "Akole", "Jamkhed", "Karjat", "Kopargaon", "Nagar", "Newasa", "Parner", "Pathardi", "Rahata", "Rahuri", "Sangamner", "Shevgaon", "Shrigonda", "Shrirampur"));
        TALUKA_MAP.put("Akola", Arrays.asList( "Akola", "Akot", "Balapur", "Barshitakli", "Murtizapur", "Patur", "Telhara"));
        TALUKA_MAP.put("Amravati", Arrays.asList( "Achalpur", "Amravati", "Anjangaon Surji", "Chandurbazar", "Chikhaldara", "Daryapur", "Dhamangaon Railway", "Morshi", "Warud"));
        TALUKA_MAP.put("Aurangabad", Arrays.asList( "Aurangabad", "Gangapur", "Kannad", "Khuldabad", "Paithan", "Phulambri", "Sillod", "Soegaon", "Vaijapur"));
        TALUKA_MAP.put("Beed", Arrays.asList( "Ambejogai", "Ashti", "Beed", "Georai", "Kaij", "Majalgaon", "Parli", "Patoda", "Shirur (Kasar)", "Wadwani"));
        TALUKA_MAP.put("Bhandara", Arrays.asList( "Bhandara", "Lakhandur", "Lakhani", "Mohadi", "Pauni", "Sakoli", "Tumsar"));
        TALUKA_MAP.put("Buldhana", Arrays.asList( "Buldhana", "Chikhli", "Deulgaon Raja", "Jalgaon Jamod", "Khamgaon", "Lonar", "Malkapur", "Mehkar", "Motala", "Nandura", "Sangrampur", "Shegaon", "Sindkhed Raja"));
        TALUKA_MAP.put("Chandrapur", Arrays.asList( "Ballarpur", "Bhadrawati", "Brahmapuri", "Chandrapur", "Chimur", "Gondpipri", "Korpana", "Mul", "Nagbhid", "Pombhurna", "Rajura", "Saoli", "Sindewahi", "Warora"));
        TALUKA_MAP.put("Dhule", Arrays.asList( "Dhule", "Sakri", "Shirpur", "Sindkheda"));
        TALUKA_MAP.put("Gadchiroli", Arrays.asList( "Aheri", "Armori", "Bhamragad", "Chamorshi", "Desaiganj", "Dhanora", "Etapalli", "Gadchiroli", "Korchi", "Kurkheda", "Mulchera", "Sironcha"));
        TALUKA_MAP.put("Gondia", Arrays.asList( "Amgaon", "Arjuni Morgaon", "Deori", "Gondia", "Goregaon", "Sadak-Arjuni", "Salekasa", "Tirora"));
        TALUKA_MAP.put("Hingoli", Arrays.asList( "Aundha Nagnath", "Basmath", "Hingoli", "Kalamnuri", "Sengaon"));
        TALUKA_MAP.put("Jalgaon", Arrays.asList( "Amalner", "Bhadgaon", "Bhusawal", "Chalisgaon", "Chopda", "Dharangaon", "Erandol", "Jalgaon", "Jamner", "Muktainagar", "Pachora", "Parola", "Raver", "Yawal"));
        TALUKA_MAP.put("Jalna", Arrays.asList( "Ambad", "Badnapur", "Bhokardan", "Ghansawangi", "Jaffrabad", "Jalna", "Mantha", "Partur"));
        TALUKA_MAP.put("Kolhapur", Arrays.asList( "Ajra", "Bhudargad", "Chandgad", "Gadhinglaj", "Hatkanangle", "Kagal", "Karvir", "Panhala", "Radhanagari", "Shahuwadi", "Shirol"));
        TALUKA_MAP.put("Latur", Arrays.asList( "Ahmadpur", "Ausa", "Chakur", "Deoni", "Jalkot", "Latur", "Nilanga", "Renapur", "Shirur-Anantpal", "Udgir"));
        TALUKA_MAP.put("Mumbai City", Arrays.asList( "Mumbai City"));
        TALUKA_MAP.put("Mumbai Suburban", Arrays.asList( "Andheri", "Borivali", "Kurla"));
        TALUKA_MAP.put("Nagpur", Arrays.asList( "Hingna", "Kamptee", "Katol", "Kuhi", "Nagpur Rural", "Nagpur Urban", "Narkhed", "Parseoni", "Ramtek", "Saoner", "Umred"));
        TALUKA_MAP.put("Nanded", Arrays.asList( "Ardhapur", "Bhokar", "Biloli", "Deglur", "Dharmabad", "Hadgaon", "Himayatnagar", "Kandhar", "Kinwat", "Loha", "Mahur", "Mudkhed", "Mukhed", "Naigaon", "Nanded", "Umri"));
        TALUKA_MAP.put("Nandurbar", Arrays.asList( "Akkalkuwa", "Akrani", "Nandurbar", "Navapur", "Shahada", "Talode"));
        TALUKA_MAP.put("Nashik", Arrays.asList( "Baglan", "Chandwad", "Deola", "Dindori", "Igatpuri", "Kalwan", "Malegaon", "Nandgaon", "Nashik", "Niphad", "Peth", "Sinnar", "Surgana", "Trimbakeshwar", "Yeola"));
        TALUKA_MAP.put("Osmanabad", Arrays.asList( "Bhum", "Kalamb", "Lohara", "Osmanabad", "Paranda", "Tuljapur", "Umarga", "Washi"));
        TALUKA_MAP.put("Palghar", Arrays.asList( "Dahanu", "Jawhar", "Mokhada", "Palghar", "Talasari", "Vada", "Vikramgad", "Vasai"));
        TALUKA_MAP.put("Parbhani", Arrays.asList( "Gangakhed", "Jintur", "Manwath", "Palam", "Parbhani", "Pathri", "Purna", "Sailu", "Sonpeth"));
        TALUKA_MAP.put("Pune", Arrays.asList( "Ambegaon", "Baramati", "Bhor", "Daund", "Haveli", "Indapur", "Junnar", "Khed", "Maval", "Mulshi", "Pune City", "Purandar", "Shirur", "Velhe"));
        TALUKA_MAP.put("Raigad", Arrays.asList( "Alibag", "Karjat", "Khalapur", "Mahad", "Mangaon", "Mhasla", "Murud", "Panvel", "Pen", "Poladpur", "Roha", "Shrivardhan", "Sudhagad", "Tala", "Uran"));
        TALUKA_MAP.put("Ratnagiri", Arrays.asList( "Chiplun", "Dapoli", "Guhagar", "Khed", "Lanja", "Mandangad", "Ratnagiri", "Sangameshwar"));
        TALUKA_MAP.put("Sangli", Arrays.asList( "Atpadi", "Jath", "Kadegaon", "Kavathemahankal", "Khanapur", "Miraj", "Palus", "Shirala", "Tasgaon", "Walwa"));
        TALUKA_MAP.put("Satara", Arrays.asList( "Jaoli", "Karad", "Khandala", "Khatav", "Koregaon", "Mahabaleshwar", "Man", "Patan", "Phaltan", "Satara", "Wai"));
        TALUKA_MAP.put("Sindhudurg", Arrays.asList( "Devgad", "Dodamarg", "Kankavli", "Kudal", "Malwan", "Sawantwadi", "Vaibhavvadi", "Vengurla"));
        TALUKA_MAP.put("Solapur", Arrays.asList( "Akkalkot", "Barshi", "Karmala", "Madha", "Malshiras", "Mangalvedhe", "Mohol", "Pandharpur", "Sangole", "Solapur North", "Solapur South"));
        TALUKA_MAP.put("Thane", Arrays.asList( "Ambernath", "Bhiwandi", "Kalyan", "Murbad", "Shahapur", "Thane", "Ulhasnagar"));
        TALUKA_MAP.put("Wardha", Arrays.asList( "Arvi", "Ashti", "Deoli", "Hinganghat", "Karanja", "Samudrapur", "Seloo", "Wardha"));
        TALUKA_MAP.put("Washim", Arrays.asList( "Karanja", "Malegaon", "Mangrulpir", "Manora", "Risod", "Washim"));
        TALUKA_MAP.put("Yavatmal", Arrays.asList( "Arni", "Babhulgaon", "Darwha", "Digras", "Ghatanji", "Kalamb", "Mahagaon", "Maregaon", "Ner", "Pandharkaoda", "Pusad", "Ralegaon", "Umarkhed", "Wani", "Yavatmal", "Zari-Jamani"));
        TALUKA_MAP.put("Select District", Arrays.asList("Select Taluka"));

        // Initialize DEGREE_MAP (same as in LoginPage2)
    // ====================== DEGREE_MAP ======================

                DEGREE_MAP.put("Education", Arrays.asList(
                        "B.Ed", "BA B.Ed", "Other"
                ));

        DEGREE_MAP.put("Arts", Arrays.asList(
                "BA", "BA (Hons)", "Home Science", "Social Work", "Journalism", "BA LLB", "Other"
        ));

        DEGREE_MAP.put("Commerce", Arrays.asList(
                "B.Com",
                "B.Com (Hons)",
                "Chartered Accountancy (CA)",
                "Cost and Management Accountancy (CMA)",
                "Company Secretary (CS)",
                "Other"
        ));

        DEGREE_MAP.put("Engineering", Arrays.asList(
                "Computer Science Engineering (CSE)",
                "Information Technology (IT)",
                "Artificial Intelligence & Machine Learning (AIML)",
                "Data Science Engineering",
                "Cyber Security",
                "Robotics Engineering",
                "Software Engineering",
                "Computer Engineering",
                "Electronics & Communication (ECE)",
                "Electrical Engineering (EE)",
                "Electronics & Telecommunication (ENTC)",
                "Instrumentation Engineering",
                "Electrical & Electronics Engineering (EEE)",
                "Mechanical Engineering (ME)",
                "Automobile Engineering",
                "Mechatronics Engineering",
                "Production Engineering",
                "Civil Engineering (CE)",
                "Architecture (B.Arch)",
                "Structural Engineering (Specialization)",
                "Chemical Engineering",
                "Industrial Engineering",
                "Petroleum Engineering",
                "Mining Engineering",
                "Agricultural Engineering",
                "Food Technology",
                "Aerospace Engineering",
                "Aeronautical Engineering",
                "Marine Engineering",
                "Naval Architecture",
                "Environmental Engineering",
                "Textile Engineering",
                "Plastic Engineering",
                "Metallurgical Engineering",
                "Other"
        ));

// DIPLOMA — ADDED LAB & RADIOLOGY
        DEGREE_MAP.put("Diploma", Arrays.asList(
                "Diploma in Computer Engineering",
                "Diploma in Information Technology",
                "Diploma in Computer Science",
                "Diploma in Artificial Intelligence",
                "Diploma in Cyber Security",
                "Diploma in Electronics & Telecommunication (ETC / ENTC)",
                "Diploma in Electrical Engineering",
                "Diploma in Electronics Engineering",
                "Diploma in Instrumentation Engineering",
                "Diploma in Electrical & Electronics Engineering (EEE)",
                "Diploma in Mechanical Engineering",
                "Diploma in Automobile Engineering",
                "Diploma in Mechatronics",
                "Diploma in Tool & Die Making",
                "Diploma in Production Engineering",
                "Diploma in Robotics",
                "Diploma in Civil Engineering",
                "Diploma in Architecture (D.Arch)",
                "Diploma in Structural Engineering",
                "Diploma in Construction Technology",
                "Diploma in Chemical Engineering",
                "Diploma in Industrial Engineering",
                "Diploma in Petrochemical Engineering",
                "Diploma in Petroleum Engineering",
                "Diploma in Mining Engineering",
                "Diploma in Aeronautical Engineering",
                "Diploma in Aerospace Engineering",
                "Diploma in Marine Engineering",
                "Diploma in Naval Architecture",
                "Diploma in Textile Engineering",
                "Diploma in Lab Technology",
                "Diploma in Radiology",
                "Diploma in X-Ray Technology",
                "Diploma in Engineering", "Advanced Diploma", "Other"
        ));

        DEGREE_MAP.put("ITI", Arrays.asList(
                "Electrician",
                "Wireman",
                "Electronics Mechanic",
                "Instrument Mechanic",
                "Electrical Maintenance",
                "Solar Technician",
                "Fitter",
                "Turner",
                "Machinist",
                "Mechanic Motor Vehicle (MMV)",
                "Diesel Mechanic",
                "Tool & Die Maker",
                "Foundryman",
                "Welder (Gas & Electric)",
                "Computer Operator (COPA)",
                "Desktop Publishing Operator (DTP)",
                "Carpenter",
                "Mechanic Diesel Engine",
                "Mechanic Motor Cycle",
                "Fashion Design Technology",
                "Plumber",
                "Painter",
                "Plastic Processing Operator",
                "Fire & Safety",
                "Interior Decoration",
                "Printing Technology",
                "Other"
        ));

        DEGREE_MAP.put("Pharmacy", Arrays.asList(
                "B.Pharm", "D.Pharm", "Other"
        ));

        DEGREE_MAP.put("Agriculture", Arrays.asList(
                "B.Sc Agri",
                "Diploma in Agriculture",
                "Agricultural Engineering",
                "Other"
        ));

        DEGREE_MAP.put("Computer Science/IT", Arrays.asList(
                "BCA", "B.Sc IT", "B.Sc CS", "Other"
        ));

        DEGREE_MAP.put("Nursing", Arrays.asList(
                "B.Sc Nursing", "GNM", "ANM", "Other"
        ));

        DEGREE_MAP.put("Law", Arrays.asList(
                "LLB", "BA LLB", "BBA LLB", "Other"
        ));

        DEGREE_MAP.put("Veterinary", Arrays.asList(
                "BVSc", "Other"
        ));

        DEGREE_MAP.put("Journalism", Arrays.asList(
                "BA Journalism", "Mass Comm", "Other"
        ));

        DEGREE_MAP.put("Management", Arrays.asList(
                "BBA", "BMS", "Aviation", "BBA LLB", "Other"
        ));

        DEGREE_MAP.put("Hotel Management", Arrays.asList(
                "BHM",
                "Diploma in Hotel Management",
                "Hospitality",
                "Other"
        ));

        DEGREE_MAP.put("Animation & Multimedia", Arrays.asList(
                "Animation", "Animation Design", "Other"
        ));

// MEDICAL — ADDED LAB & RADIOLOGY
        DEGREE_MAP.put("Medical", Arrays.asList(
                "MBBS",
                "BAMS",
                "BHMS",
                "BUMS",
                "Lab Technology",
                "Diploma in Radiology",
                "Diploma in X-Ray Technology",
                "Other"
        ));

        DEGREE_MAP.put("Dental", Arrays.asList(
                "BDS", "Other"
        ));

// OTHER B.Sc — Clean
        DEGREE_MAP.put("Other B.Sc", Arrays.asList(
                "Physics", "Chemistry", "Maths", "Biology",
                "Biotech", "Home Science", "MLT", "Radiology",
                "Stats", "Geology", "Env Science", "Other"
        ));

// OTHER — Clean
        DEGREE_MAP.put("Other", Arrays.asList(
                "Planning", "Fashion", "Interior",
                "Fine Arts", "Painting", "Sculpture", "Applied Arts",
                "Music", "Dance", "Theatre",
                "Physiotherapy", "Social Work", "Other"
        ));


        // Initialize POST_GRAD_MAP (same as in LoginPage2)
        POST_GRAD_MAP.put("Education", Arrays.asList( "M.Ed", "None"));
        POST_GRAD_MAP.put("Arts", Arrays.asList( "MA", "None"));
        POST_GRAD_MAP.put("Commerce", Arrays.asList( "M.Com", "MBA", "None"));
        POST_GRAD_MAP.put("Engineering", Arrays.asList( "M.Tech", "M.E", "MBA", "None"));
        POST_GRAD_MAP.put("Diploma", Arrays.asList( "Advanced Diploma", "None"));
        POST_GRAD_MAP.put("Medical", Arrays.asList( "MD", "MS", "M.Sc", "None"));
        POST_GRAD_MAP.put("Dental", Arrays.asList( "MDS", "None"));
        POST_GRAD_MAP.put("ITI", Arrays.asList( "None"));
        POST_GRAD_MAP.put("Pharmacy", Arrays.asList( "M.Pharm", "None"));
        POST_GRAD_MAP.put("Agriculture", Arrays.asList( "M.Sc Agri", "None"));
        POST_GRAD_MAP.put("Computer Science/IT", Arrays.asList( "MCA", "M.Sc IT", "M.Tech", "None"));
        POST_GRAD_MAP.put("Nursing", Arrays.asList( "M.Sc Nursing", "None"));
        POST_GRAD_MAP.put("Law", Arrays.asList( "LLM", "None"));
        POST_GRAD_MAP.put("Veterinary", Arrays.asList( "MVSc", "None"));
        POST_GRAD_MAP.put("Journalism", Arrays.asList( "MA Journalism", "None"));
        POST_GRAD_MAP.put("Management", Arrays.asList( "MBA", "PGDM", "None"));
        POST_GRAD_MAP.put("Hotel Management", Arrays.asList( "MHM", "MBA Hospitality", "None"));
        POST_GRAD_MAP.put("Animation & Multimedia", Arrays.asList( "M.Sc Animation", "None"));
        POST_GRAD_MAP.put("Other B.Sc", Arrays.asList( "M.Sc", "None"));
        POST_GRAD_MAP.put("Other", Arrays.asList( "None"));
    }
}
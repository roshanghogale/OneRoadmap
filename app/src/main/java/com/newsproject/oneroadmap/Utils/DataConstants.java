package com.newsproject.oneroadmap.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataConstants {
    public static final List<String> DISTRICTS = Arrays.asList(
            "Select District", "Ahmednagar", "Akola", "Amravati", "Aurangabad", "Beed",
            "Bhandara", "Buldhana", "Chandrapur", "Dhule", "Gadchiroli", "Gondia",
            "Hingoli", "Jalgaon", "Jalna", "Kolhapur", "Latur", "Mumbai City",
            "Mumbai Suburban", "Nagpur", "Nanded", "Nandurbar", "Nashik", "Osmanabad",
            "Palghar", "Parbhani", "Pune", "Raigad", "Ratnagiri", "Sangli",
            "Satara", "Sindhudurg", "Solapur", "Thane", "Wardha", "Washim", "Yavatmal"
    );

    public static final Map<String, List<String>> TALUKA_MAP = new HashMap<>();

    public static final List<String> EDUCATION_OPTIONS = Arrays.asList(
            "Select Education Category",
            "Engineering", "Medical", "Dental", "Pharmacy", "Nursing", "Paramedical",
            "Biotechnology", "Agriculture", "Veterinary", "Law", "Management", "Commerce",
            "Arts", "Pure Science", "Computer Science/IT", "Architecture", "Hotel Management",
            "Design", "Journalism", "Education", "Polytechnic", "ITI", "Home Science",
            "Performing Arts", "Visual Arts", "Animation & Multimedia", "Aviation",
            "Social Work", "Other"
    );

    public static final List<String> TWELFTH_OPTIONS = Arrays.asList(
            "Select 12th Stream", "Science", "Commerce", "Arts", "Vocational", "None"
    );

    public static final Map<String, List<String>> DEGREE_MAP = new HashMap<>();
    public static final Map<String, List<String>> POST_GRAD_MAP = new HashMap<>();

    static {
        // Initialize TALUKA_MAP (same as in LoginPage3)
        TALUKA_MAP.put("Ahmednagar", Arrays.asList("Select Taluka", "Akole", "Jamkhed", "Karjat", "Kopargaon", "Nagar", "Newasa", "Parner", "Pathardi", "Rahata", "Rahuri", "Sangamner", "Shevgaon", "Shrigonda", "Shrirampur"));
        TALUKA_MAP.put("Akola", Arrays.asList("Select Taluka", "Akola", "Akot", "Balapur", "Barshitakli", "Murtizapur", "Patur", "Telhara"));
        TALUKA_MAP.put("Amravati", Arrays.asList("Select Taluka", "Achalpur", "Amravati", "Anjangaon Surji", "Chandurbazar", "Chikhaldara", "Daryapur", "Dhamangaon Railway", "Morshi", "Warud"));
        TALUKA_MAP.put("Aurangabad", Arrays.asList("Select Taluka", "Aurangabad", "Gangapur", "Kannad", "Khuldabad", "Paithan", "Phulambri", "Sillod", "Soegaon", "Vaijapur"));
        TALUKA_MAP.put("Beed", Arrays.asList("Select Taluka", "Ambejogai", "Ashti", "Beed", "Georai", "Kaij", "Majalgaon", "Parli", "Patoda", "Shirur (Kasar)", "Wadwani"));
        TALUKA_MAP.put("Bhandara", Arrays.asList("Select Taluka", "Bhandara", "Lakhandur", "Lakhani", "Mohadi", "Pauni", "Sakoli", "Tumsar"));
        TALUKA_MAP.put("Buldhana", Arrays.asList("Select Taluka", "Buldhana", "Chikhli", "Deulgaon Raja", "Jalgaon Jamod", "Khamgaon", "Lonar", "Malkapur", "Mehkar", "Motala", "Nandura", "Sangrampur", "Shegaon", "Sindkhed Raja"));
        TALUKA_MAP.put("Chandrapur", Arrays.asList("Select Taluka", "Ballarpur", "Bhadrawati", "Brahmapuri", "Chandrapur", "Chimur", "Gondpipri", "Korpana", "Mul", "Nagbhid", "Pombhurna", "Rajura", "Saoli", "Sindewahi", "Warora"));
        TALUKA_MAP.put("Dhule", Arrays.asList("Select Taluka", "Dhule", "Sakri", "Shirpur", "Sindkheda"));
        TALUKA_MAP.put("Gadchiroli", Arrays.asList("Select Taluka", "Aheri", "Armori", "Bhamragad", "Chamorshi", "Desaiganj", "Dhanora", "Etapalli", "Gadchiroli", "Korchi", "Kurkheda", "Mulchera", "Sironcha"));
        TALUKA_MAP.put("Gondia", Arrays.asList("Select Taluka", "Amgaon", "Arjuni Morgaon", "Deori", "Gondia", "Goregaon", "Sadak-Arjuni", "Salekasa", "Tirora"));
        TALUKA_MAP.put("Hingoli", Arrays.asList("Select Taluka", "Aundha Nagnath", "Basmath", "Hingoli", "Kalamnuri", "Sengaon"));
        TALUKA_MAP.put("Jalgaon", Arrays.asList("Select Taluka", "Amalner", "Bhadgaon", "Bhusawal", "Chalisgaon", "Chopda", "Dharangaon", "Erandol", "Jalgaon", "Jamner", "Muktainagar", "Pachora", "Parola", "Raver", "Yawal"));
        TALUKA_MAP.put("Jalna", Arrays.asList("Select Taluka", "Ambad", "Badnapur", "Bhokardan", "Ghansawangi", "Jaffrabad", "Jalna", "Mantha", "Partur"));
        TALUKA_MAP.put("Kolhapur", Arrays.asList("Select Taluka", "Ajra", "Bhudargad", "Chandgad", "Gadhinglaj", "Hatkanangle", "Kagal", "Karvir", "Panhala", "Radhanagari", "Shahuwadi", "Shirol"));
        TALUKA_MAP.put("Latur", Arrays.asList("Select Taluka", "Ahmadpur", "Ausa", "Chakur", "Deoni", "Jalkot", "Latur", "Nilanga", "Renapur", "Shirur-Anantpal", "Udgir"));
        TALUKA_MAP.put("Mumbai City", Arrays.asList("Select Taluka", "Mumbai City"));
        TALUKA_MAP.put("Mumbai Suburban", Arrays.asList("Select Taluka", "Andheri", "Borivali", "Kurla"));
        TALUKA_MAP.put("Nagpur", Arrays.asList("Select Taluka", "Hingna", "Kamptee", "Katol", "Kuhi", "Nagpur Rural", "Nagpur Urban", "Narkhed", "Parseoni", "Ramtek", "Saoner", "Umred"));
        TALUKA_MAP.put("Nanded", Arrays.asList("Select Taluka", "Ardhapur", "Bhokar", "Biloli", "Deglur", "Dharmabad", "Hadgaon", "Himayatnagar", "Kandhar", "Kinwat", "Loha", "Mahur", "Mudkhed", "Mukhed", "Naigaon", "Nanded", "Umri"));
        TALUKA_MAP.put("Nandurbar", Arrays.asList("Select Taluka", "Akkalkuwa", "Akrani", "Nandurbar", "Navapur", "Shahada", "Talode"));
        TALUKA_MAP.put("Nashik", Arrays.asList("Select Taluka", "Baglan", "Chandwad", "Deola", "Dindori", "Igatpuri", "Kalwan", "Malegaon", "Nandgaon", "Nashik", "Niphad", "Peth", "Sinnar", "Surgana", "Trimbakeshwar", "Yeola"));
        TALUKA_MAP.put("Osmanabad", Arrays.asList("Select Taluka", "Bhum", "Kalamb", "Lohara", "Osmanabad", "Paranda", "Tuljapur", "Umarga", "Washi"));
        TALUKA_MAP.put("Palghar", Arrays.asList("Select Taluka", "Dahanu", "Jawhar", "Mokhada", "Palghar", "Talasari", "Vada", "Vikramgad", "Vasai"));
        TALUKA_MAP.put("Parbhani", Arrays.asList("Select Taluka", "Gangakhed", "Jintur", "Manwath", "Palam", "Parbhani", "Pathri", "Purna", "Sailu", "Sonpeth"));
        TALUKA_MAP.put("Pune", Arrays.asList("Select Taluka", "Ambegaon", "Baramati", "Bhor", "Daund", "Haveli", "Indapur", "Junnar", "Khed", "Maval", "Mulshi", "Pune City", "Purandar", "Shirur", "Velhe"));
        TALUKA_MAP.put("Raigad", Arrays.asList("Select Taluka", "Alibag", "Karjat", "Khalapur", "Mahad", "Mangaon", "Mhasla", "Murud", "Panvel", "Pen", "Poladpur", "Roha", "Shrivardhan", "Sudhagad", "Tala", "Uran"));
        TALUKA_MAP.put("Ratnagiri", Arrays.asList("Select Taluka", "Chiplun", "Dapoli", "Guhagar", "Khed", "Lanja", "Mandangad", "Ratnagiri", "Sangameshwar"));
        TALUKA_MAP.put("Sangli", Arrays.asList("Select Taluka", "Atpadi", "Jath", "Kadegaon", "Kavathemahankal", "Khanapur", "Miraj", "Palus", "Shirala", "Tasgaon", "Walwa"));
        TALUKA_MAP.put("Satara", Arrays.asList("Select Taluka", "Jaoli", "Karad", "Khandala", "Khatav", "Koregaon", "Mahabaleshwar", "Man", "Patan", "Phaltan", "Satara", "Wai"));
        TALUKA_MAP.put("Sindhudurg", Arrays.asList("Select Taluka", "Devgad", "Dodamarg", "Kankavli", "Kudal", "Malwan", "Sawantwadi", "Vaibhavvadi", "Vengurla"));
        TALUKA_MAP.put("Solapur", Arrays.asList("Select Taluka", "Akkalkot", "Barshi", "Karmala", "Madha", "Malshiras", "Mangalvedhe", "Mohol", "Pandharpur", "Sangole", "Solapur North", "Solapur South"));
        TALUKA_MAP.put("Thane", Arrays.asList("Select Taluka", "Ambernath", "Bhiwandi", "Kalyan", "Murbad", "Shahapur", "Thane", "Ulhasnagar"));
        TALUKA_MAP.put("Wardha", Arrays.asList("Select Taluka", "Arvi", "Ashti", "Deoli", "Hinganghat", "Karanja", "Samudrapur", "Seloo", "Wardha"));
        TALUKA_MAP.put("Washim", Arrays.asList("Select Taluka", "Karanja", "Malegaon", "Mangrulpir", "Manora", "Risod", "Washim"));
        TALUKA_MAP.put("Yavatmal", Arrays.asList("Select Taluka", "Arni", "Babhulgaon", "Darwha", "Digras", "Ghatanji", "Kalamb", "Mahagaon", "Maregaon", "Ner", "Pandharkaoda", "Pusad", "Ralegaon", "Umarkhed", "Wani", "Yavatmal", "Zari-Jamani"));
        TALUKA_MAP.put("Select District", Arrays.asList("Select Taluka"));

        // Initialize DEGREE_MAP (same as in LoginPage2)
        DEGREE_MAP.put("Engineering", Arrays.asList("Select Degree", "B.Tech", "B.E", "B.Sc Engineering", "Diploma in Engineering", "Other"));
        DEGREE_MAP.put("Medical", Arrays.asList("Select Degree", "MBBS", "BAMS", "BHMS", "BUMS", "Other"));
        DEGREE_MAP.put("Dental", Arrays.asList("Select Degree", "BDS", "Other"));
        DEGREE_MAP.put("Pharmacy", Arrays.asList("Select Degree", "B.Pharm", "D.Pharm", "Other"));
        DEGREE_MAP.put("Nursing", Arrays.asList("Select Degree", "B.Sc Nursing", "GNM", "ANM", "Other"));
        DEGREE_MAP.put("Paramedical", Arrays.asList("Select Degree", "BPT", "B.Sc MLT", "B.Sc Radiology", "Other"));
        DEGREE_MAP.put("Biotechnology", Arrays.asList("Select Degree", "B.Sc Biotechnology", "B.Tech Biotechnology", "Other"));
        DEGREE_MAP.put("Agriculture", Arrays.asList("Select Degree", "B.Sc Agriculture", "B.Tech Agriculture", "Other"));
        DEGREE_MAP.put("Veterinary", Arrays.asList("Select Degree", "BVSc", "Other"));
        DEGREE_MAP.put("Law", Arrays.asList("Select Degree", "LLB", "BA LLB", "BBA LLB", "Other"));
        DEGREE_MAP.put("Management", Arrays.asList("Select Degree", "BBA", "BMS", "Other"));
        DEGREE_MAP.put("Commerce", Arrays.asList("Select Degree", "B.Com", "B.Com (Hons)", "Other"));
        DEGREE_MAP.put("Arts", Arrays.asList("Select Degree", "BA", "BA (Hons)", "Other"));
        DEGREE_MAP.put("Pure Science", Arrays.asList("Select Degree", "B.Sc Physics", "B.Sc Chemistry", "B.Sc Maths", "B.Sc Biology", "Other"));
        DEGREE_MAP.put("Computer Science/IT", Arrays.asList("Select Degree", "BCA", "B.Sc IT", "B.Sc Computer Science", "Other"));
        DEGREE_MAP.put("Architecture", Arrays.asList("Select Degree", "B.Arch", "Other"));
        DEGREE_MAP.put("Hotel Management", Arrays.asList("Select Degree", "BHM", "B.Sc Hospitality", "Other"));
        DEGREE_MAP.put("Design", Arrays.asList("Select Degree", "B.Des Fashion", "B.Des Interior", "BFA", "Other"));
        DEGREE_MAP.put("Journalism", Arrays.asList("Select Degree", "BA Journalism", "B.Sc Mass Comm", "Other"));
        DEGREE_MAP.put("Education", Arrays.asList("Select Degree", "B.Ed", "BA B.Ed", "Other"));
        DEGREE_MAP.put("Polytechnic", Arrays.asList("Select Degree", "Diploma in Civil", "Diploma in Mechanical", "Diploma in Electrical", "Other"));
        DEGREE_MAP.put("ITI", Arrays.asList("Select Degree", "ITI in Electrician", "ITI in Fitter", "ITI in Welder", "Other"));
        DEGREE_MAP.put("Home Science", Arrays.asList("Select Degree", "B.Sc Home Science", "BA Home Science", "Other"));
        DEGREE_MAP.put("Performing Arts", Arrays.asList("Select Degree", "BPA Music", "BPA Dance", "BPA Theatre", "Other"));
        DEGREE_MAP.put("Visual Arts", Arrays.asList("Select Degree", "BFA Painting", "BFA Sculpture", "BFA Applied Arts", "Other"));
        DEGREE_MAP.put("Animation & Multimedia", Arrays.asList("Select Degree", "B.Sc Animation", "B.Des Animation", "Other"));
        DEGREE_MAP.put("Aviation", Arrays.asList("Select Degree", "B.Sc Aviation", "BBA Aviation", "Other"));
        DEGREE_MAP.put("Social Work", Arrays.asList("Select Degree", "BSW", "BA Social Work", "Other"));
        DEGREE_MAP.put("Other", Arrays.asList("Select Degree", "Other"));

        // Initialize POST_GRAD_MAP (same as in LoginPage2)
        POST_GRAD_MAP.put("Engineering", Arrays.asList("Select Post Graduation", "M.Tech", "M.E", "MBA", "None"));
        POST_GRAD_MAP.put("Medical", Arrays.asList("Select Post Graduation", "MD", "MS", "M.Sc", "None"));
        POST_GRAD_MAP.put("Dental", Arrays.asList("Select Post Graduation", "MDS", "None"));
        POST_GRAD_MAP.put("Pharmacy", Arrays.asList("Select Post Graduation", "M.Pharm", "None"));
        POST_GRAD_MAP.put("Nursing", Arrays.asList("Select Post Graduation", "M.Sc Nursing", "None"));
        POST_GRAD_MAP.put("Paramedical", Arrays.asList("Select Post Graduation", "MPT", "M.Sc", "None"));
        POST_GRAD_MAP.put("Biotechnology", Arrays.asList("Select Post Graduation", "M.Sc Biotechnology", "M.Tech", "None"));
        POST_GRAD_MAP.put("Agriculture", Arrays.asList("Select Post Graduation", "M.Sc Agriculture", "None"));
        POST_GRAD_MAP.put("Veterinary", Arrays.asList("Select Post Graduation", "MVSc", "None"));
        POST_GRAD_MAP.put("Law", Arrays.asList("Select Post Graduation", "LLM", "None"));
        POST_GRAD_MAP.put("Management", Arrays.asList("Select Post Graduation", "MBA", "PGDM", "None"));
        POST_GRAD_MAP.put("Commerce", Arrays.asList("Select Post Graduation", "M.Com", "MBA", "None"));
        POST_GRAD_MAP.put("Arts", Arrays.asList("Select Post Graduation", "MA", "None"));
        POST_GRAD_MAP.put("Pure Science", Arrays.asList("Select Post Graduation", "M.Sc", "None"));
        POST_GRAD_MAP.put("Computer Science/IT", Arrays.asList("Select Post Graduation", "MCA", "M.Sc IT", "None"));
        POST_GRAD_MAP.put("Architecture", Arrays.asList("Select Post Graduation", "M.Arch", "None"));
        POST_GRAD_MAP.put("Hotel Management", Arrays.asList("Select Post Graduation", "MHM", "MBA Hospitality", "None"));
        POST_GRAD_MAP.put("Design", Arrays.asList("Select Post Graduation", "M.Des", "None"));
        POST_GRAD_MAP.put("Journalism", Arrays.asList("Select Post Graduation", "MA Journalism", "None"));
        POST_GRAD_MAP.put("Education", Arrays.asList("Select Post Graduation", "M.Ed", "None"));
        POST_GRAD_MAP.put("Polytechnic", Arrays.asList("Select Post Graduation", "Advanced Diploma", "None"));
        POST_GRAD_MAP.put("ITI", Arrays.asList("Select Post Graduation", "None"));
        POST_GRAD_MAP.put("Home Science", Arrays.asList("Select Post Graduation", "M.Sc Home Science", "None"));
        POST_GRAD_MAP.put("Performing Arts", Arrays.asList("Select Post Graduation", "MPA", "None"));
        POST_GRAD_MAP.put("Visual Arts", Arrays.asList("Select Post Graduation", "MFA", "None"));
        POST_GRAD_MAP.put("Animation & Multimedia", Arrays.asList("Select Post Graduation", "M.Sc Animation", "None"));
        POST_GRAD_MAP.put("Aviation", Arrays.asList("Select Post Graduation", "M.Sc Aviation", "MBA Aviation", "None"));
        POST_GRAD_MAP.put("Social Work", Arrays.asList("Select Post Graduation", "MSW", "None"));
        POST_GRAD_MAP.put("Other", Arrays.asList("Select Post Graduation", "None"));
    }
}
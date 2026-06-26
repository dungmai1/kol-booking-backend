UPDATE kol_profile
SET avatar_url = CASE id
        WHEN 258 THEN 'https://media.licdn.com/dms/image/v2/D5603AQF69UW0V2dRbA/profile-displayphoto-scale_200_200/B56ZlXPYf6IsAY-/0/1758105283263?e=2147483647&v=beta&t=AVnhHQcnjeCgJbzezn3C_ZNVLWjJyVxSp4S6cQjvdlU'
        WHEN 259 THEN 'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSM6r3_Jjz1YImDCIgPpmTragFtDaiXjlSa7XaNPdrzERfHYNnLVPdAYK4&s=10'
    END,
    updated_at = NOW()
WHERE id IN (258, 259);

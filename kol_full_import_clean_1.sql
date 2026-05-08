-- ======================================================================
-- CLEANUP (OPTIONAL): Remove previously imported data before re-importing
-- ======================================================================
-- The IMPORT block below is now idempotent (ON CONFLICT / NOT EXISTS),
-- so you can safely skip this CLEANUP block and re-run only the IMPORT.
-- Run CLEANUP only when you want a clean reset of imported rows.
--
-- Order matters because of FK constraints:
--   kol_social_channel -> kol_profile -> app_user
-- We delete children first, then parents.

BEGIN;

-- 1. Delete social channels belonging to imported KOLs
DELETE FROM kol_social_channel
WHERE kol_profile_id IN (
    SELECT kp.id
    FROM kol_profile kp
    JOIN app_user au ON au.id = kp.user_id
    WHERE au.email LIKE '%@kols-koc.imported'
);

-- 2. Delete kol_profile rows belonging to imported users
DELETE FROM kol_profile
WHERE user_id IN (
    SELECT id FROM app_user WHERE email LIKE '%@kols-koc.imported'
);

-- 3. Delete the imported app_user rows
DELETE FROM app_user WHERE email LIKE '%@kols-koc.imported';

COMMIT;

-- ======================================================================
-- IMPORT: Insert app_user + kol_profile + kol_social_channel
-- ======================================================================

-- ======================================================================
-- 3-tier import: app_user + kol_profile + kol_social_channel
--   108 users / KOL profiles
--   273 social channels
-- Source: https://kols-koc.com/all-influencers/ (pages 1-9 of 21)
-- ======================================================================

BEGIN;

-- Stage 1: Stage user input. 'rn' is a stable row number that we will
-- use to map app_user.id -> kol_profile, then kol_profile.id -> social channels.
WITH user_input (rn, email, display_name, slug, bio) AS (
    VALUES
        (1, 'ngo.thi.kim.yen@kols-koc.imported', 'Ngô Thị Kim Yến', 'ngo-thi-kim-yen', 'Phân loại: KOC, life style, Tiktoker

Lĩnh vực: Nhà hàng, Phong cách sống, Thời trang'),
        (2, 'linh.baeli@kols-koc.imported', 'Linh Baeli', 'linh-baeli', 'Phân loại: Fashionista, KOL, Reviewer

Lĩnh vực: Mỹ phẩm, Phong cách sống, Thời trang'),
        (3, 'nhan.phuong.chi.xu@kols-koc.imported', 'Nhân Phương Chị Xu', 'nhan-phuong-chi-xu', 'Phân loại: Gia đình, Tiktoker

Lĩnh vực: Gia đình, Mẹ và bé, Phong cách sống'),
        (4, 'bu.ne@kols-koc.imported', 'Bư nè', 'bu-ne', 'Phân loại: Reviewer, Tiktoker

Lĩnh vực: Nhà hàng, Phong cách sống'),
        (5, 'hat.tieu.foodie@kols-koc.imported', 'Hạt Tiêu Foodie', 'hat-tieu-foodie', 'Phân loại: Reviewer, Tiktoker

Lĩnh vực: Nhà hàng, Phong cách sống'),
        (6, 'quynh.tran.ne@kols-koc.imported', 'Quỳnh Trân nè', 'quynh-tran-ne', 'Phân loại: KOC, Reviewer, Tiktoker

Lĩnh vực: Phong cách sống, Thời trang'),
        (7, 'nha.hieu.review@kols-koc.imported', 'Nhà Hiếu Review', 'nha-hieu-review', 'Phân loại: Gia đình, Mẹ và bé

Lĩnh vực: Gia đình, Mẹ và bé, Mỹ phẩm'),
        (8, 'meembeshin@kols-koc.imported', 'meembeshin', 'meembeshin', 'Phân loại: Gia đình, Mẹ và bé

Lĩnh vực: Gia đình, Mẹ và bé'),
        (9, 'medaureview078@kols-koc.imported', 'medaureview078', 'medaureview078', 'Phân loại: Gia đình, Mẹ và bé

Lĩnh vực: Gia đình, Mẹ và bé, Sức khỏe'),
        (10, 'simple.man@kols-koc.imported', 'Simple Man', 'simple-man', 'Phân loại: Fashionista, Tiktoker

Lĩnh vực: Phong cách sống, Thời trang'),
        (11, 'gia.dinh.mat.hi@kols-koc.imported', 'Gia Đình Mắt Hí', 'gia-dinh-mat-hi', 'Lĩnh vực: Gia đình, Mỹ phẩm'),
        (12, 'duc.va.ly@kols-koc.imported', 'Đức và Ly', 'duc-va-ly', 'Phân loại: Gia đình, Influencer, Reviewer

Lĩnh vực: Gia đình, Phong cách sống'),
        (13, 'di.di@kols-koc.imported', 'Di Di', 'di-di', 'Phân loại: Influencer, KOL, Reviewer

Lĩnh vực: Mỹ phẩm, Phong cách sống, Thời trang'),
        (14, 'chi.ho.vlog@kols-koc.imported', 'Chi Hồ vlog', 'chi-ho-vlog', 'Phân loại: KOL, Reviewer

Lĩnh vực: Mỹ phẩm, Phong cách sống, Thời trang'),
        (15, 'duong.jin@kols-koc.imported', 'Dương Jin', 'duong-jin', 'Phân loại: Influencer, KOL, Reviewer

Lĩnh vực: Mỹ phẩm, Phong cách sống, Thời trang'),
        (16, 'le.kha.duy@kols-koc.imported', 'Lê Kha Duy', 'le-kha-duy', 'Phân loại: Beauty Blogger, Influencer

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (17, 'yul.daily@kols-koc.imported', 'Yul Daily', 'yul-daily', 'Phân loại: Beauty Blogger, Beauty review, KOL

Lĩnh vực: Mỹ phẩm, Phong cách sống, Thời trang'),
        (18, 'ha.cookie@kols-koc.imported', 'Hà cookie', 'ha-cookie', 'Phân loại: Beauty review, Reviewer

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (19, 'hanyone@kols-koc.imported', 'Hanyone', 'hanyone', 'Phân loại: KOL, Makeup artist

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (20, 'me.chang.va.bap@kols-koc.imported', 'Mẹ Chang và Bắp', 'me-chang-va-bap', 'Phân loại: Tiktoker

Lĩnh vực: Gia đình, Mẹ và bé'),
        (21, 'nhamomtuyen@kols-koc.imported', 'nhamomtuyen', 'nhamomtuyen', 'Phân loại: Gia đình, Mẹ và bé

Lĩnh vực: Gia đình, Phong cách sống, Thời trang'),
        (22, 'phuong.quynh.me.gau.vani@kols-koc.imported', 'Phương Quỳnh – Mẹ Gấu&Vani', 'phuong-quynh-me-gau-vani', 'Phân loại: Gia đình, Mẹ và bé

Lĩnh vực: Gia đình, Phong cách sống, Thời trang'),
        (23, 'mebaoboi93@kols-koc.imported', 'mebaoboi93', 'mebaoboi93', 'Phân loại: Gia đình, Mẹ và bé

Lĩnh vực: Gia đình, Mẹ và bé'),
        (24, 'tieu.ngoc@kols-koc.imported', 'Tiêu Ngọc', 'tieu-ngoc', 'Phân loại: Influencer, Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống, Thời trang'),
        (25, 'cim.ngan@kols-koc.imported', 'Cim Ngân', 'cim-ngan', 'Phân loại: Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống, Thời trang'),
        (26, 'ridung.iuoilaiu@kols-koc.imported', 'Ridung iuoilaiu', 'ridung-iuoilaiu', 'Phân loại: Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống, Thời trang'),
        (27, 'tuan.anh.le@kols-koc.imported', 'Tuấn Anh Lê', 'tuan-anh-le', 'Phân loại: Tiktoker

Lĩnh vực: Gia đình, Mỹ phẩm, Phong cách sống, Thời trang'),
        (28, 'thau.di.dau@kols-koc.imported', 'Thâu Đi Đâu', 'thau-di-dau', 'Phân loại: KOC, Tiktoker, Youtuber

Lĩnh vực: Phong cách sống, Thực phẩm'),
        (29, 'lie.makeup@kols-koc.imported', 'Lie Makeup', 'lie-makeup', 'Phân loại: KOL, Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (30, 'mim.chong@kols-koc.imported', 'Mim Chông', 'mim-chong', 'Phân loại: Tiktoker

Lĩnh vực: Gia đình, Mỹ phẩm, Phong cách sống, Thời trang'),
        (31, 'hach.lien.tu.nguyet@kols-koc.imported', 'Hách Liên Tử Nguyệt', 'hach-lien-tu-nguyet', 'Phân loại: KOL, Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống, Thời trang'),
        (32, 'review.cung.bum@kols-koc.imported', 'Review cùng Bum', 'review-cung-bum', 'Phân loại: Tiktoker

Lĩnh vực: Mỹ phẩm, Nhà hàng, Phong cách sống'),
        (33, 'me.ho@kols-koc.imported', 'Mẹ Hổ', 'me-ho', 'Phân loại: Tiktoker

Lĩnh vực: Gia đình, Mẹ và bé'),
        (34, 'khanh.van@kols-koc.imported', 'Khánh Vân', 'khanh-van', 'Phân loại: KOL, Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (35, 'phi.yen.pham@kols-koc.imported', 'Phi Yến Phạm', 'phi-yen-pham', 'Phân loại: Tiktoker

Lĩnh vực: Gym và thể thao, Mỹ phẩm, Phong cách sống, Sức khỏe'),
        (36, 'tu.thuy.review@kols-koc.imported', 'Tú Thụy Review', 'tu-thuy-review', 'Phân loại: Beauty review, Tiktoker

Lĩnh vực: Mỹ phẩm, Thời trang'),
        (37, 'an.di.an@kols-koc.imported', 'An Đi Ăn', 'an-di-an', 'Phân loại: Influencer, KOL

Lĩnh vực: Bloggers, Thực phẩm'),
        (38, 'doan.quoc.ha.nhi@kols-koc.imported', 'Doãn Quốc Hạ Nhi', 'doan-quoc-ha-nhi', 'Phân loại: Beauty Blogger, Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (39, 'anh.tuyet.yen.nhi@kols-koc.imported', 'Ánh Tuyết Nè Quý Dị – Yến Nhi', 'anh-tuyet-yen-nhi', 'Phân loại: Reviewer, Tiktoker

Lĩnh vực: Nhà hàng, Phong cách sống'),
        (40, 'chill.with.dan@kols-koc.imported', 'Chill With Dan', 'chill-with-dan', 'Phân loại: Reviewer, Tiktoker

Lĩnh vực: Công nghệ'),
        (41, 'truc.quynh.junmi@kols-koc.imported', 'Trúc Quỳnh Junmi', 'truc-quynh-junmi', 'Phân loại: Reviewer, Tiktoker

Lĩnh vực: Phong cách sống'),
        (42, 'yoshi.vu@kols-koc.imported', 'Yoshi Vũ', 'yoshi-vu', 'Phân loại: Influencer, KOL

Lĩnh vực: Phong cách sống'),
        (43, 'thin.duc@kols-koc.imported', 'Thìn Đức', 'thin-duc', 'Phân loại: Fashionista, Gia đình, Influencer

Lĩnh vực: Gia đình, Phong cách sống'),
        (44, 'linhsieuxinh@kols-koc.imported', 'Linhsieuxinh', 'linhsieuxinh', 'Phân loại: Tiktoker

Lĩnh vực: Phong cách sống, Thời trang'),
        (45, 'ly.pham@kols-koc.imported', 'Ly Phạm', 'ly-pham', 'Phân loại: Tiktoker

Lĩnh vực: Phong cách sống'),
        (46, 'dumi@kols-koc.imported', 'DuMi', 'dumi', 'Phân loại: KOL, Reviewer

Lĩnh vực: Phong cách sống, Thời trang'),
        (47, 'sammy@kols-koc.imported', 'Sammy', 'sammy', 'Phân loại: Tiktoker

Lĩnh vực: Thực phẩm'),
        (48, 'quyenleodaily@kols-koc.imported', 'Quyenleodaily', 'quyenleodaily', 'Phân loại: Gia đình, KOL

Lĩnh vực: Nhà hàng, Phong cách sống'),
        (49, 'yen.thich.review@kols-koc.imported', 'Yến Thích Review', 'yen-thich-review', 'Phân loại: KOC, Tiktoker, Youtuber

Lĩnh vực: Du lịch, Phong cách sống, Thời trang, Thực phẩm'),
        (50, 'hy.khi.duong.duong@kols-koc.imported', 'Hỷ Khí Dương Dương', 'hy-khi-duong-duong', 'Phân loại: Reviewer, Tiktoker

Lĩnh vực: Nhà hàng, Phong cách sống, Thực phẩm'),
        (51, 'hong.anh@kols-koc.imported', 'Hồng Ánh', 'hong-anh', 'Phân loại: KOL, Tiktoker

Lĩnh vực: Phong cách sống'),
        (52, 'baby.tra.sua@kols-koc.imported', 'Baby Trà Sữa', 'baby-tra-sua', 'Phân loại: Tiktoker, Youtuber

Lĩnh vực: Gia đình, Mẹ và bé'),
        (53, 'em.be.noi.tieng.anh.minhee@kols-koc.imported', 'Em bé nói tiếng Anh MinHee', 'em-be-noi-tieng-anh-minhee', 'Phân loại: Tiktoker

Lĩnh vực: Gia đình, Mẹ và bé'),
        (54, 'mam.non.giai.tri@kols-koc.imported', 'Mầm Non Giải Trí', 'mam-non-giai-tri', 'Phân loại: Tiktoker, Youtuber

Lĩnh vực: Gia đình, Mẹ và bé'),
        (55, 'inlil.chang.nguyen@kols-koc.imported', 'Inlil (Chang Nguyen)', 'inlil-chang-nguyen', 'Phân loại: Beauty Blogger, Makeup artist, Tiktoker

Lĩnh vực: Bloggers, Mỹ phẩm'),
        (56, 'doan.van.anh@kols-koc.imported', 'Đoàn Vân Anh', 'doan-van-anh', 'Phân loại: Beauty review, KOL

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (57, 'quynh.nhi@kols-koc.imported', 'Quỳnh Nhi', 'quynh-nhi', 'Phân loại: Beauty Blogger, Tiktoker

Lĩnh vực: Bloggers, Mỹ phẩm'),
        (58, 'hong.khanh@kols-koc.imported', 'Hồng Khanh', 'hong-khanh', 'Phân loại: KOL, Tiktoker

Lĩnh vực: Phong cách sống'),
        (59, 'hoang.kim.chi@kols-koc.imported', 'Hoàng Kim Chi', 'hoang-kim-chi', 'Phân loại: KOL, Tiktoker

Lĩnh vực: Bloggers, Mỹ phẩm, Phong cách sống, Thời trang'),
        (60, 'me.bim.mio@kols-koc.imported', 'Mẹ Bỉm Mio', 'me-bim-mio', 'Phân loại: Tiktoker

Lĩnh vực: Gia đình, Mẹ và bé'),
        (61, 'me.kem.review@kols-koc.imported', 'Mẹ Kem Review', 'me-kem-review', 'Phân loại: Tiktoker

Lĩnh vực: Gia đình, Mẹ và bé'),
        (62, 'blacki.d@kols-koc.imported', 'Blacki D', 'blacki-d', 'Phân loại: Tiktoker

Lĩnh vực: Mỹ phẩm, Thời trang'),
        (63, 'quynh.thi@kols-koc.imported', 'Quỳnh Thi', 'quynh-thi', 'Phân loại: Beauty Blogger, Tiktoker, Youtuber

Lĩnh vực: Mỹ phẩm, Thời trang'),
        (64, 'lou.le@kols-koc.imported', 'Lou Lê', 'lou-le', 'Phân loại: Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống, Sức khỏe'),
        (65, 'yen.dan@kols-koc.imported', 'Yên Đan', 'yen-dan', 'Phân loại: Tiktoker

Lĩnh vực: Du lịch, Mỹ phẩm, Thời trang'),
        (66, 'nhan.di.an@kols-koc.imported', 'Nhân đi ăn', 'nhan-di-an', 'Phân loại: Tiktoker

Lĩnh vực: Bloggers, Nhà hàng'),
        (67, 'ong.anh.thich.nau.an@kols-koc.imported', 'Ông Anh thích nấu ăn', 'ong-anh-thich-nau-an', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng, Phong cách sống, Vloggers'),
        (68, 'babykopo.home@kols-koc.imported', 'Babykopo Home', 'babykopo-home', 'Phân loại: Tiktoker, Youtuber

Lĩnh vực: Mẹ và bé, Nhà hàng, Vloggers'),
        (69, 'saigon.tastes@kols-koc.imported', 'Saigon Tastes', 'saigon-tastes', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (70, 'sonnhi@kols-koc.imported', 'sonnhi', 'sonnhi', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (71, 'phomai.an.ramen@kols-koc.imported', 'phomai ăn ramen', 'phomai-an-ramen', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (72, 'yoohee.beauty@kols-koc.imported', 'Yoohee Beauty', 'yoohee-beauty', 'Phân loại: Tiktoker

Lĩnh vực: Mỹ phẩm'),
        (73, 'miu.linh@kols-koc.imported', 'Miu Linh', 'miu-linh', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng, Phong cách sống'),
        (74, 'ba.chua.review@kols-koc.imported', 'Bà chúa review', 'ba-chua-review', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (75, 'dia.diem.an.uong@kols-koc.imported', 'Địa điểm ăn uống', 'dia-diem-an-uong', 'Phân loại: Tiktoker

Lĩnh vực: Bloggers, Nhà hàng'),
        (76, 'huyen.tu@kols-koc.imported', 'Huyền Tú', 'huyen-tu', 'Phân loại: KOC, Tiktoker

Lĩnh vực: Du lịch, Mỹ phẩm, Phong cách sống'),
        (77, 'di.dau.nao@kols-koc.imported', 'Đi Đâu Nào', 'di-dau-nao', 'Phân loại: Tiktoker

Lĩnh vực: Du lịch, Phong cách sống'),
        (78, 'hanbow.beauty@kols-koc.imported', 'Hanbow Beauty', 'hanbow-beauty', 'Phân loại: Beauty Blogger, Tiktoker

Lĩnh vực: Mỹ phẩm'),
        (79, 'ha.noi.an.gi@kols-koc.imported', 'Hà Nội Ăn Gì', 'ha-noi-an-gi', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (80, 'bui.thi.kim.luyen@kols-koc.imported', 'Bùi Thị Kim Luyến', 'bui-thi-kim-luyen', 'Phân loại: Beauty Blogger, Tiktoker

Lĩnh vực: Mẹ và bé, Mỹ phẩm, Phong cách sống'),
        (81, 'lan.cung.bean@kols-koc.imported', 'LĂN CÙNG BEAN', 'lan-cung-bean', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (82, 'vy.pinky@kols-koc.imported', 'Vy Pinky', 'vy-pinky', 'Phân loại: Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (83, 'hazen@kols-koc.imported', 'Hazen', 'hazen', 'Phân loại: KOL

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (84, 'dung.amy@kols-koc.imported', 'Dung Amy', 'dung-amy', 'Phân loại: KOC, MC

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (85, 'nha.cua.bi@kols-koc.imported', 'Nhà của Bí', 'nha-cua-bi', 'Phân loại: Tiktoker

Lĩnh vực: Gia đình, Mẹ và bé, Phong cách sống'),
        (86, 'thu.uyen@kols-koc.imported', 'Thu Uyên', 'thu-uyen', 'Phân loại: Influencer, KOL, Tiktoker

Lĩnh vực: Mỹ phẩm, Nhà hàng, Phong cách sống'),
        (87, 'hoang.thi.phuong.thao@kols-koc.imported', 'Hoàng Thị Phương Thảo', 'hoang-thi-phuong-thao', 'Phân loại: Influencer, MC

Lĩnh vực: Bloggers, Phong cách sống'),
        (88, 'trinh.thi.my.le@kols-koc.imported', 'Trịnh Thị Mỹ Lệ', 'trinh-thi-my-le', 'Phân loại: KOL

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (89, 'gia.dinh.mat.mat@kols-koc.imported', 'Gia đình Mật Mật', 'gia-dinh-mat-mat', 'Phân loại: Tiktoker

Lĩnh vực: Công nghệ, Gia đình, Mẹ và bé, Phong cách sống'),
        (90, 'huyen.nguyen.hmeuu@kols-koc.imported', 'Huyền Nguyễn – Hmeuu', 'huyen-nguyen-hmeuu', 'Phân loại: KOL, Tiktoker

Lĩnh vực: Phong cách sống, Thời trang'),
        (91, 'tuyen.pham.family@kols-koc.imported', 'Tuyền Phạm Family', 'tuyen-pham-family', 'Phân loại: KOL

Lĩnh vực: Gia đình, Mẹ và bé'),
        (92, 'thu.nhi.eat.clean.hong@kols-koc.imported', 'Thu Nhi – Eat Clean Hông', 'thu-nhi-eat-clean-hong', 'Phân loại: Tiktoker, Youtuber

Lĩnh vực: Phong cách sống'),
        (93, 'eatwhning@kols-koc.imported', 'Eatwhning', 'eatwhning', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (94, 'meily@kols-koc.imported', 'Meily', 'meily', 'Phân loại: Reviewer

Lĩnh vực: Du lịch, Nhà hàng, Phong cách sống'),
        (95, 'thanh.lan@kols-koc.imported', 'Thanh Lan', 'thanh-lan', 'Phân loại: Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống, Thời trang'),
        (96, 'hai.wanderlust@kols-koc.imported', 'Hải Wanderlust', 'hai-wanderlust', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (97, 'ng.le.thu.thuy@kols-koc.imported', 'Ng Lê Thu Thuỷ', 'ng-le-thu-thuy', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (98, 'di.cung.thy@kols-koc.imported', 'Đi Cùng Thy', 'di-cung-thy', 'Phân loại: Tiktoker, Youtuber

Lĩnh vực: Du lịch'),
        (99, 'pham.ha.phuong.thao@kols-koc.imported', 'Phạm Hà Phương Thảo', 'pham-ha-phuong-thao', 'Phân loại: Beauty review, Tiktoker

Lĩnh vực: Mỹ phẩm'),
        (100, 'sak.food.an.gi.choi.gi@kols-koc.imported', 'Sak Food ăn gì chơi gì', 'sak-food-an-gi-choi-gi', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (101, 'nghia.tinon@kols-koc.imported', 'Nghĩa Tinon', 'nghia-tinon', 'Phân loại: Reviewer, Tiktoker

Lĩnh vực: Du lịch, Nhà hàng'),
        (102, 'quin.vu.thuy.quynh@kols-koc.imported', 'QUIN Vũ Thúy Quỳnh', 'quin-vu-thuy-quynh', 'Phân loại: Beauty Blogger, Youtuber

Lĩnh vực: Mỹ phẩm'),
        (103, 'henry.tran@kols-koc.imported', 'Henry Tran', 'henry-tran', 'Phân loại: Tiktoker

Lĩnh vực: Du lịch, Mỹ phẩm, Phong cách sống'),
        (104, 'fancy.with.trang@kols-koc.imported', 'Fancy With Trang', 'fancy-with-trang', 'Phân loại: Tiktoker

Lĩnh vực: Nhà hàng'),
        (105, 'lee.hay.di@kols-koc.imported', 'LEE hay đi', 'lee-hay-di', 'Phân loại: Tiktoker

Lĩnh vực: Du lịch, Nhà hàng, Phong cách sống'),
        (106, 'doan.thanh.ngan@kols-koc.imported', 'Đoàn Thanh Ngân', 'doan-thanh-ngan', 'Phân loại: Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống'),
        (107, 'nguyen.thoai.nghi@kols-koc.imported', 'Nguyễn Thoại Nghi', 'nguyen-thoai-nghi', 'Phân loại: KOL

Lĩnh vực: Phong cách sống, Thời trang'),
        (108, 'quynh.go@kols-koc.imported', 'Quỳnh Gờ', 'quynh-go', 'Phân loại: Tiktoker

Lĩnh vực: Mỹ phẩm, Phong cách sống')
),
-- Insert app_user only when email doesn't exist yet (idempotent re-run)
inserted_users AS (
    INSERT INTO app_user (email, password_hash, role, status, email_verified)
    SELECT
        ui.email,
        '$2a$10$IMPORTED.placeholder.hash.replace.before.use................',
        'KOL',
        'PENDING_VERIFICATION',
        false
    FROM user_input ui
    ORDER BY ui.rn
    ON CONFLICT (email) DO NOTHING
    RETURNING id, email
),
-- Resolve every input email to its app_user.id. Postgres CTEs share one
-- snapshot, so SELECTs cannot see rows just inserted by inserted_users;
-- we UNION the RETURNING set with the pre-existing rows.
all_users AS (
    SELECT id, email FROM inserted_users
    UNION ALL
    SELECT au.id, au.email
    FROM app_user au
    JOIN user_input ui ON ui.email = au.email
),
-- Insert kol_profile only when user has no profile and slug is free
inserted_profiles AS (
    INSERT INTO kol_profile (user_id, display_name, slug, bio, status, avg_rating, review_count)
    SELECT
        au.id,
        ui.display_name,
        ui.slug,
        ui.bio,
        'DRAFT',
        0,
        0
    FROM user_input ui
    JOIN all_users au ON au.email = ui.email
    WHERE NOT EXISTS (SELECT 1 FROM kol_profile kp WHERE kp.user_id = au.id)
    ON CONFLICT (slug) DO NOTHING
    RETURNING id AS profile_id, slug
),
-- Resolve every input slug to its kol_profile.id. Same snapshot caveat as
-- all_users above: SELECT from kol_profile cannot see inserted_profiles,
-- so we UNION the RETURNING set with the pre-existing rows.
all_profiles AS (
    SELECT profile_id, slug FROM inserted_profiles
    UNION ALL
    SELECT kp.id AS profile_id, kp.slug
    FROM kol_profile kp
    JOIN user_input ui ON ui.slug = kp.slug
),
social_input (slug, platform, url, username, follower_count) AS (
    VALUES
        ('ngo-thi-kim-yen', 'tiktok', 'https://www.tiktok.com/@yenyew.tiktok', 'yenyew.tiktok', 376000),
        ('ngo-thi-kim-yen', 'facebook', 'https://www.facebook.com/yenneh2000', 'yenneh2000', 102000),
        ('linh-baeli', 'tiktok', 'https://www.tiktok.com/@linhbaelii', 'linhbaelii', 360000),
        ('linh-baeli', 'facebook', 'https://www.facebook.com/linhbaeli', 'linhbaeli', 12000),
        ('linh-baeli', 'instagram', 'https://www.instagram.com/linhbaeli/', 'linhbaeli', 7000),
        ('nhan-phuong-chi-xu', 'facebook', 'https://www.facebook.com/nhanphuongchixu2', 'nhanphuongchixu2', 61000),
        ('nhan-phuong-chi-xu', 'tiktok', 'https://www.tiktok.com/@nhanphuongchixu?lang=vi-VN', 'nhanphuongchixu', 619000),
        ('bu-ne', 'tiktok', 'https://www.tiktok.com/@buanaodian?_t=8eUoqHG1lQX&_r=1', 'buanaodian', 126000),
        ('bu-ne', 'instagram', 'https://www.instagram.com/biteallfood/', 'biteallfood', 22000),
        ('hat-tieu-foodie', 'tiktok', 'https://www.tiktok.com/@hattieufoodie?lang=en', 'hattieufoodie', 759000),
        ('hat-tieu-foodie', 'facebook', 'https://www.facebook.com/profile.php?id=100067131511200', 'profile_100067131511200', 19000),
        ('quynh-tran-ne', 'tiktok', 'https://www.tiktok.com/@helloquynhtrannee?_t=8eUoIRjAQzp&_r=1', 'helloquynhtrannee', 11500),
        ('quynh-tran-ne', 'instagram', 'https://www.instagram.com/helloquynhtranne/', 'helloquynhtranne', 12000),
        ('nha-hieu-review', 'tiktok', 'https://www.tiktok.com/@mocmoc898', 'mocmoc898', 215100),
        ('meembeshin', 'facebook', 'https://www.facebook.com/voduychieuanh2611', 'voduychieuanh2611', 160000),
        ('meembeshin', 'tiktok', 'https://www.tiktok.com/@meembeshin', 'meembeshin', 63000),
        ('medaureview078', 'facebook', 'https://www.facebook.com/fiona.neu.78', 'fiona.neu.78', 4000),
        ('medaureview078', 'tiktok', 'https://www.tiktok.com/@medaureview078', 'medaureview078', 29000),
        ('medaureview078', 'instagram', 'https://www.instagram.com/fiona078/', 'fiona078', 5000),
        ('simple-man', 'tiktok', 'https://www.tiktok.com/@simplemanlegacy?_t=8efErcKzB8D&_r=1', 'simplemanlegacy', 160200),
        ('simple-man', 'instagram', 'https://www.instagram.com/vasualise/', 'vasualise', 100000),
        ('gia-dinh-mat-hi', 'tiktok', 'https://www.tiktok.com/@giadinhmathi3', 'giadinhmathi3', 128400),
        ('duc-va-ly', 'facebook', 'https://www.facebook.com/ducliduli', 'ducliduli', 195000),
        ('duc-va-ly', 'instagram', 'https://www.instagram.com/imlii1302/', 'imlii1302', 4100),
        ('duc-va-ly', 'tiktok', 'https://www.tiktok.com/@ducli_duli', 'ducli_duli', 746000),
        ('duc-va-ly', 'youtube', 'https://www.youtube.com/channel/UC0MKpdNYaLt-O0d5biDV6vg', 'UC0MKpdNYaLt-O0d5biDV6vg', 1300),
        ('di-di', 'instagram', 'https://www.instagram.com/tyberry_033/', 'tyberry_033', 120000),
        ('di-di', 'tiktok', 'https://www.tiktok.com/@tyberry_', 'tyberry_', 1300000),
        ('chi-ho-vlog', 'facebook', 'https://www.facebook.com/hotham.8/', 'hotham.8', 1200),
        ('chi-ho-vlog', 'youtube', 'https://www.youtube.com/c/ChiH%E1%BB%93vlog', 'ChiHồvlog', 7900),
        ('chi-ho-vlog', 'tiktok', 'https://www.tiktok.com/@chihovlog', 'chihovlog', 223000),
        ('duong-jin', 'facebook', 'https://www.facebook.com/profile.php?id=100009530620078', 'profile_100009530620078', 5000),
        ('duong-jin', 'instagram', 'https://www.instagram.com/sunflizme', 'sunflizme', 12500),
        ('duong-jin', 'tiktok', 'https://www.tiktok.com/@sunflizme?lang=en', 'sunflizme', 1300000),
        ('le-kha-duy', 'facebook', 'https://www.facebook.com/khaduyle01', 'khaduyle01', 102000),
        ('le-kha-duy', 'instagram', 'https://www.instagram.com/Khaduyle_makeup', 'Khaduyle_makeup', 2200),
        ('le-kha-duy', 'tiktok', 'https://www.tiktok.com/@khaduyle', 'khaduyle', 204000),
        ('yul-daily', 'tiktok', 'https://www.tiktok.com/@yuldaily', 'yuldaily', 574000),
        ('yul-daily', 'youtube', 'https://www.youtube.com/channel/UCO7265dNn-xdMqdEBJulUcQ', 'UCO7265dNn-xdMqdEBJulUcQ', 11300),
        ('ha-cookie', 'facebook', 'https://www.facebook.com/Hminhhuyen.03', 'Hminhhuyen.03', 61700),
        ('ha-cookie', 'instagram', 'https://www.instagram.com/hacookie_', 'hacookie_', 276000),
        ('ha-cookie', 'tiktok', 'https://www.tiktok.com/@hacookiereview_', 'hacookiereview_', 1000000),
        ('hanyone', 'facebook', 'https://www.facebook.com/hanyone08', 'hanyone08', 1500),
        ('hanyone', 'instagram', 'https://www.instagram.com/hanyyone', 'hanyyone', 94800),
        ('hanyone', 'tiktok', 'https://www.tiktok.com/@_hanyone_', '_hanyone_', 126000),
        ('me-chang-va-bap', 'tiktok', 'https://www.tiktok.com/@mechang.bap?lang=vi-VN', 'mechang.bap', 19100),
        ('nhamomtuyen', 'facebook', 'https://www.facebook.com/2703tc', '2703tc', 200000),
        ('nhamomtuyen', 'tiktok', 'https://www.tiktok.com/@nhamomtuyen', 'nhamomtuyen', 409000),
        ('phuong-quynh-me-gau-vani', 'facebook', 'https://www.facebook.com/KUNBONNY/', 'KUNBONNY', 249000),
        ('phuong-quynh-me-gau-vani', 'tiktok', 'https://www.tiktok.com/@quinnsosynh', 'quinnsosynh', 233000),
        ('phuong-quynh-me-gau-vani', 'instagram', 'https://www.instagram.com/quinnsosynh/', 'quinnsosynh', 37000),
        ('mebaoboi93', 'facebook', 'https://www.facebook.com/2703tc', '2703tc', 200000),
        ('mebaoboi93', 'tiktok', 'https://www.tiktok.com/@nhamomtuyen', 'nhamomtuyen', 409000),
        ('tieu-ngoc', 'facebook', 'https://www.facebook.com/tieungocbu1', 'tieungocbu1', 866000),
        ('tieu-ngoc', 'instagram', 'https://www.instagram.com/tieu.ngoc.bu', 'tieu.ngoc.bu', 40600),
        ('tieu-ngoc', 'tiktok', 'https://www.tiktok.com/@tieungocbuofficial?lang=vi-VN', 'tieungocbuofficial', 603000),
        ('cim-ngan', 'tiktok', 'https://www.tiktok.com/@cimngan0503?_t=8efEXMVbcwM&_r=1', 'cimngan0503', 1300000),
        ('cim-ngan', 'facebook', 'https://www.facebook.com/kimnganteamtrieuditu', 'kimnganteamtrieuditu', 207000),
        ('ridung-iuoilaiu', 'tiktok', 'https://www.tiktok.com/@babimilo_', 'babimilo_', 2000000),
        ('ridung-iuoilaiu', 'instagram', 'https://www.instagram.com/rithuydung/?hl=vi', 'rithuydung', 102000),
        ('tuan-anh-le', 'tiktok', 'https://www.tiktok.com/@lee.bitu?_t=8efiesb4xNK&_r=1', 'lee.bitu', 379400),
        ('tuan-anh-le', 'instagram', 'https://www.instagram.com/lee.bitu/', 'lee.bitu', 10400),
        ('thau-di-dau', 'tiktok', 'https://www.tiktok.com/@thaudidau?lang=vi-VN', 'thaudidau', 232000),
        ('thau-di-dau', 'facebook', 'https://www.facebook.com/thaudidau', 'thaudidau', 130000),
        ('thau-di-dau', 'youtube', 'https://www.youtube.com/c/ThâuĐiĐâu', 'ThâuĐiĐâu', 66600),
        ('thau-di-dau', 'instagram', 'https://www.instagram.com/thaudidau/', 'thaudidau', 1100),
        ('lie-makeup', 'facebook', 'https://www.facebook.com/lielie3004', 'lielie3004', 92000),
        ('lie-makeup', 'instagram', 'https://www.instagram.com/lie.m.u.a/', 'lie.m.u.a', 2700),
        ('lie-makeup', 'tiktok', 'https://www.tiktok.com/@lielie300401', 'lielie300401', 644000),
        ('lie-makeup', 'youtube', 'https://www.youtube.com/channel/UCsFH5_Tuz_QQoHtxOvNwlEQ', 'UCsFH5_Tuz_QQoHtxOvNwlEQ', 533000),
        ('mim-chong', 'facebook', 'https://www.facebook.com/nha.mimchong/', 'nha.mimchong', 91000),
        ('mim-chong', 'tiktok', 'https://www.tiktok.com/@nha.mimchong', 'nha.mimchong', 436000),
        ('mim-chong', 'youtube', 'https://www.youtube.com/channel/UCu0VCWAW1cO9RZDS57NUZVA', 'UCu0VCWAW1cO9RZDS57NUZVA', 3710),
        ('mim-chong', 'instagram', 'https://www.instagram.com/nha.mongchim', 'nha.mongchim', 41400),
        ('hach-lien-tu-nguyet', 'facebook', 'https://www.facebook.com/nguyetmingg', 'nguyetmingg', 33000),
        ('hach-lien-tu-nguyet', 'instagram', 'https://www.instagram.com/hachlientunguyet', 'hachlientunguyet', 219000),
        ('hach-lien-tu-nguyet', 'tiktok', 'https://www.tiktok.com/@Ming0910', 'Ming0910', 1800000),
        ('hach-lien-tu-nguyet', 'youtube', 'https://www.youtube.com/channel/UC2WlY_zBc59uxu8oW56JXtg', 'UC2WlY_zBc59uxu8oW56JXtg', 23600),
        ('review-cung-bum', 'facebook', 'https://www.facebook.com/kun.pi.1', 'kun.pi.1', 104000),
        ('review-cung-bum', 'tiktok', 'https://www.tiktok.com/@duonghung_04?_t=8VvWqODIlCU&_r=1', 'duonghung_04', 186000),
        ('me-ho', 'tiktok', 'https://www.tiktok.com/@mebeho2022?lang=vi-VN', 'mebeho2022', 36200),
        ('khanh-van', 'tiktok', 'https://www.tiktok.com/@khvan19', 'khvan19', 1700000),
        ('khanh-van', 'instagram', 'https://www.instagram.com/khvan191', 'khvan191', 311000),
        ('khanh-van', 'facebook', 'https://www.facebook.com/khanhvan.DNL', 'khanhvan.DNL', 69000),
        ('phi-yen-pham', 'facebook', 'https://www.facebook.com/imneyy', 'imneyy', 92000),
        ('phi-yen-pham', 'tiktok', 'https://www.tiktok.com/@imneyy?_t=8epLaxuWELQ&_r=1', 'imneyy', 155000),
        ('phi-yen-pham', 'instagram', 'https://www.instagram.com/imneyy', 'imneyy', 60900),
        ('tu-thuy-review', 'facebook', 'https://www.facebook.com/thanhtuthuy3003', 'thanhtuthuy3003', 101000),
        ('tu-thuy-review', 'tiktok', 'https://www.tiktok.com/@thanhtuthuy?_t=8eUoOnek7CE&_r=1', 'thanhtuthuy', 151000),
        ('tu-thuy-review', 'instagram', 'https://www.instagram.com/Thanhtu.thuy', 'Thanhtu.thuy', 16300),
        ('an-di-an', 'facebook', 'https://www.facebook.com/vogiaan.98', 'vogiaan.98', 311000),
        ('an-di-an', 'tiktok', 'https://www.tiktok.com/@anvyfood', 'anvyfood', 861900),
        ('an-di-an', 'instagram', 'https://www.instagram.com/_vogiaan73/', '_vogiaan73', 79700),
        ('doan-quoc-ha-nhi', 'tiktok', 'https://www.tiktok.com/@draconids.mf1111', 'draconids.mf1111', 515200),
        ('doan-quoc-ha-nhi', 'facebook', 'https://www.facebook.com/jetkyus', 'jetkyus', 26500),
        ('doan-quoc-ha-nhi', 'instagram', 'https://www.instagram.com/draconids.mf1111/', 'draconids.mf1111', 27400),
        ('anh-tuyet-yen-nhi', 'facebook', 'https://www.facebook.com/nhiyennguyen1905', 'nhiyennguyen1905', 155000),
        ('anh-tuyet-yen-nhi', 'instagram', 'https://www.instagram.com/nynn_1905/', 'nynn_1905', 348000),
        ('anh-tuyet-yen-nhi', 'tiktok', 'https://www.tiktok.com/@nynn_1905', 'nynn_1905', 384000),
        ('chill-with-dan', 'facebook', 'https://www.facebook.com/dananh0112/', 'dananh0112', 13300),
        ('chill-with-dan', 'instagram', 'https://www.instagram.com/chill_with_dan/', 'chill_with_dan', 16100),
        ('chill-with-dan', 'tiktok', 'https://www.tiktok.com/@chill_with_dan', 'chill_with_dan', 89200),
        ('chill-with-dan', 'youtube', 'https://youtube.com/@ChillWithDan', 'ChillWithDan', 16200),
        ('truc-quynh-junmi', 'facebook', 'https://www.facebook.com/trucquynh.junmi', 'trucquynh.junmi', 267000),
        ('truc-quynh-junmi', 'instagram', 'https://www.instagram.com/trucquynhjunmi', 'trucquynhjunmi', 23700),
        ('truc-quynh-junmi', 'tiktok', 'https://www.tiktok.com/@trucquynhjunmi', 'trucquynhjunmi', 304000),
        ('truc-quynh-junmi', 'youtube', 'https://www.youtube.com/channel/UCzt_gmCMGK3EUFZJ5Dgge-g', 'UCzt_gmCMGK3EUFZJ5Dgge-g', 28400),
        ('linhsieuxinh', 'tiktok', 'https://www.tiktok.com/@linhsieuxinhhh/', 'linhsieuxinhhh', 1600000),
        ('linhsieuxinh', 'facebook', 'https://www.facebook.com/profile.php?id=100078527275880', 'profile_100078527275880', 16000),
        ('ly-pham', 'tiktok', 'https://www.tiktok.com/@lypham.2001', 'lypham.2001', 1100000),
        ('ly-pham', 'tiktok', 'https://www.tiktok.com/@motcauchuyendai3', 'motcauchuyendai3', 43200),
        ('ly-pham', 'instagram', 'https://www.instagram.com/lypham.2001', 'lypham.2001', 420000),
        ('ly-pham', 'facebook', 'https://www.facebook.com/lypham2001', 'lypham2001', 33000),
        ('sammy', 'tiktok', 'https://www.tiktok.com/@sammy.becool', 'sammy.becool', 1700000),
        ('sammy', 'facebook', 'https://www.facebook.com/Sammycooking', 'Sammycooking', 283000),
        ('quyenleodaily', 'facebook', 'https://facebook.com/laquocquyen', 'laquocquyen', 115000),
        ('quyenleodaily', 'instagram', 'https://www.instagram.com/quyenleodaily', 'quyenleodaily', 80700),
        ('quyenleodaily', 'tiktok', 'https://www.tiktok.com/@quyenleodaily', 'quyenleodaily', 3800000),
        ('quyenleodaily', 'youtube', 'https://www.youtube.com/@quyenleodaily', 'quyenleodaily', 644000),
        ('yen-thich-review', 'tiktok', 'https://www.tiktok.com/@yenncd_review', 'yenncd_review', 755300),
        ('yen-thich-review', 'tiktok', 'https://www.tiktok.com/@yennoicomdien', 'yennoicomdien', 2600000),
        ('yen-thich-review', 'instagram', 'https://www.instagram.com/yennoicomdien/', 'yennoicomdien', 3000),
        ('yen-thich-review', 'facebook', 'https://www.facebook.com/NBHY17982001', 'NBHY17982001', 73000),
        ('yen-thich-review', 'youtube', 'https://www.youtube.com/@yenthichreview', 'yenthichreview', 646000),
        ('yen-thich-review', 'youtube', 'https://www.youtube.com/@yennoicomdien', 'yennoicomdien', 1640000),
        ('hy-khi-duong-duong', 'facebook', 'https://www.facebook.com/ypl.mel', 'ypl.mel', 545000),
        ('hy-khi-duong-duong', 'instagram', 'https://www.instagram.com/ypl.mel/', 'ypl.mel', 21500),
        ('hy-khi-duong-duong', 'tiktok', 'https://www.tiktok.com/@hykhiduongduong', 'hykhiduongduong', 1400000),
        ('hy-khi-duong-duong', 'youtube', 'https://www.youtube.com/channel/UChTBwzXyCM9BBsTUACiiMew', 'UChTBwzXyCM9BBsTUACiiMew', 14700),
        ('hong-anh', 'tiktok', 'https://www.tiktok.com/@honganhh712', 'honganhh712', 1200000),
        ('hong-anh', 'instagram', 'https://www.instagram.com/hhonganhhh/', 'hhonganhhh', 7000),
        ('hong-anh', 'facebook', 'https://www.facebook.com/hhonganhhh', 'hhonganhhh', 73000),
        ('baby-tra-sua', 'facebook', 'https://www.facebook.com/babytrasua', 'babytrasua', 150000),
        ('baby-tra-sua', 'youtube', 'https://www.youtube.com/@baby.trasua', 'baby.trasua', 4620),
        ('baby-tra-sua', 'tiktok', 'https://www.tiktok.com/@babytrasua', 'babytrasua', 750800),
        ('em-be-noi-tieng-anh-minhee', 'facebook', 'https://www.facebook.com/growwithminhee', 'growwithminhee', 632000),
        ('em-be-noi-tieng-anh-minhee', 'youtube', 'https://www.youtube.com/@growwithminhee.official', 'growwithminhee.official', 53000),
        ('em-be-noi-tieng-anh-minhee', 'tiktok', 'https://www.tiktok.com/@growwithminhee', 'growwithminhee', 1100000),
        ('mam-non-giai-tri', 'facebook', 'https://www.facebook.com/mamn0ngiaitri', 'mamn0ngiaitri', 63000),
        ('mam-non-giai-tri', 'youtube', 'https://www.youtube.com/@mamnongiaitri9860', 'mamnongiaitri9860', 961),
        ('mam-non-giai-tri', 'instagram', 'https://www.instagram.com/mamn0ngiaitri/', 'mamn0ngiaitri', 386),
        ('mam-non-giai-tri', 'tiktok', 'https://www.tiktok.com/@mamn0n.giaitri', 'mamn0n.giaitri', 235600),
        ('inlil-chang-nguyen', 'facebook', 'https://www.facebook.com/nguyen.chang.1213986', 'nguyen.chang.1213986', 44000),
        ('inlil-chang-nguyen', 'tiktok', 'https://www.tiktok.com/@_inlil', '_inlil', 1600000),
        ('inlil-chang-nguyen', 'instagram', 'https://www.instagram.com/_inlil', '_inlil', 187000),
        ('inlil-chang-nguyen', 'youtube', 'https://www.youtube.com/@inlil2542', 'inlil2542', 900),
        ('doan-van-anh', 'instagram', 'https://www.instagram.com/van.jinka/', 'van.jinka', 14200),
        ('doan-van-anh', 'tiktok', 'https://www.tiktok.com/@vanjinka', 'vanjinka', 658300),
        ('quynh-nhi', 'tiktok', 'https://www.tiktok.com/@quynhitraan', 'quynhitraan', 2100000),
        ('quynh-nhi', 'facebook', 'https://www.facebook.com/tran.jesal', 'tran.jesal', 15000),
        ('quynh-nhi', 'instagram', 'https://www.instagram.com/quynhitraan/', 'quynhitraan', 230000),
        ('quynh-nhi', 'youtube', 'https://www.youtube.com/@QuynhNhiTranquynhitraan', 'QuynhNhiTranquynhitraan', 82400),
        ('hong-khanh', 'tiktok', 'https://www.tiktok.com/@nghggkhanh', 'nghggkhanh', 1100000),
        ('hong-khanh', 'facebook', 'https://www.facebook.com/HongKhanh.CTHP', 'HongKhanh.CTHP', 133000),
        ('hong-khanh', 'instagram', 'https://www.instagram.com/khanhpinkk/', 'khanhpinkk', 226000),
        ('hoang-kim-chi', 'tiktok', 'https://www.tiktok.com/@chikimhoang0310', 'chikimhoang0310', 2400000),
        ('hoang-kim-chi', 'facebook', 'https://www.facebook.com/chikimhoang310', 'chikimhoang310', 170000),
        ('hoang-kim-chi', 'instagram', 'https://www.instagram.com/chrissy_hoang/', 'chrissy_hoang', 112000),
        ('me-bim-mio', 'tiktok', 'https://www.tiktok.com/@memio07', 'memio07', 50700),
        ('me-kem-review', 'tiktok', 'https://www.tiktok.com/@thomreview', 'thomreview', 102600),
        ('blacki-d', 'facebook', 'https://www.facebook.com/daothibac/', 'daothibac', 136000),
        ('blacki-d', 'tiktok', 'https://www.tiktok.com/@blackii_d', 'blackii_d', 287500),
        ('blacki-d', 'instagram', 'https://www.instagram.com/blackiii_d', 'blackiii_d', 167000),
        ('quynh-thi', 'facebook', 'https://www.facebook.com/quynhthie1998', 'quynhthie1998', 124000),
        ('quynh-thi', 'tiktok', 'https://www.tiktok.com/@msquynhthie', 'msquynhthie', 2100000),
        ('quynh-thi', 'youtube', 'https://www.youtube.com/c/quynhthi', 'quynhthi', 1390000),
        ('quynh-thi', 'instagram', 'https://www.instagram.com/quynhthie/', 'quynhthie', 897000),
        ('lou-le', 'facebook', 'https://www.facebook.com/loule.okela/', 'loule.okela', 99000),
        ('lou-le', 'tiktok', 'https://www.tiktok.com/@lou.le', 'lou.le', 638600),
        ('lou-le', 'instagram', 'https://www.instagram.com/loule______/', 'loule______', 11100),
        ('yen-dan', 'facebook', 'https://www.facebook.com/andienchannel', 'andienchannel', 48000),
        ('yen-dan', 'tiktok', 'https://www.tiktok.com/@yendan7', 'yendan7', 380700),
        ('yen-dan', 'instagram', 'https://www.instagram.com/doyendan', 'doyendan', 13200),
        ('nhan-di-an', 'facebook', 'https://www.facebook.com/nhandian/', 'nhandian', 257000),
        ('nhan-di-an', 'tiktok', 'https://www.tiktok.com/@nhandian', 'nhandian', 689400),
        ('nhan-di-an', 'youtube', 'https://www.youtube.com/channel/UC8wZWvVFV9BPLJnSMmGGdrw', 'UC8wZWvVFV9BPLJnSMmGGdrw', 79),
        ('nhan-di-an', 'instagram', 'https://www.instagram.com/nhandian', 'nhandian', 14200),
        ('ong-anh-thich-nau-an', 'facebook', 'https://www.facebook.com/onganhthichnauan', 'onganhthichnauan', 1600000),
        ('ong-anh-thich-nau-an', 'tiktok', 'https://www.tiktok.com/@genjadao', 'genjadao', 2700000),
        ('ong-anh-thich-nau-an', 'youtube', 'https://www.youtube.com/c/ôngAnhthíchnấuăn', 'ôngAnhthíchnấuăn', 517000),
        ('ong-anh-thich-nau-an', 'instagram', 'https://www.instagram.com/onganh.thichnauan/', 'onganh.thichnauan', 61800),
        ('babykopo-home', 'facebook', 'https://www.facebook.com/babykopohome.page', 'babykopohome.page', 1600000),
        ('babykopo-home', 'tiktok', 'https://www.tiktok.com/@babykopohome', 'babykopohome', 6600000),
        ('babykopo-home', 'youtube', 'https://www.youtube.com/@BABYKOPOHOME', 'BABYKOPOHOME', 1400000),
        ('babykopo-home', 'instagram', 'https://www.instagram.com/babykopohome/', 'babykopohome', 467000),
        ('saigon-tastes', 'tiktok', 'https://www.tiktok.com/@saigontastes', 'saigontastes', 98800),
        ('sonnhi', 'tiktok', 'https://www.tiktok.com/@sonn.hineeeee', 'sonn.hineeeee', 2000),
        ('phomai-an-ramen', 'tiktok', 'https://www.tiktok.com/@phomai.anramen', 'phomai.anramen', 14900),
        ('yoohee-beauty', 'tiktok', 'https://www.tiktok.com/@yooheebeauty', 'yooheebeauty', 56000),
        ('yoohee-beauty', 'facebook', 'https://www.facebook.com/YooHeeBeauty', 'YooHeeBeauty', 21000),
        ('miu-linh', 'tiktok', 'https://www.tiktok.com/@miulinhmap', 'miulinhmap', 264000),
        ('miu-linh', 'facebook', 'https://www.facebook.com/Linhmiusmile', 'Linhmiusmile', 1000),
        ('miu-linh', 'instagram', 'https://www.instagram.com/Linhmiusmile/', 'Linhmiusmile', 54000),
        ('ba-chua-review', 'tiktok', 'https://www.tiktok.com/@bachuareview', 'bachuareview', 329000),
        ('ba-chua-review', 'instagram', 'https://www.instagram.com/bachuaviahe/', 'bachuaviahe', 221000),
        ('dia-diem-an-uong', 'tiktok', 'https://www.tiktok.com/@diadiemanuong', 'diadiemanuong', 418000),
        ('huyen-tu', 'tiktok', 'https://www.tiktok.com/@huyentuday', 'huyentuday', 619900),
        ('huyen-tu', 'facebook', 'https://www.facebook.com/profile.php?id=100092105274753', 'profile_100092105274753', 33000),
        ('huyen-tu', 'instagram', 'https://www.instagram.com/huyentuu/', 'huyentuu', 6700),
        ('di-dau-nao', 'tiktok', 'https://www.tiktok.com/@didaunao2022', 'didaunao2022', 134000),
        ('di-dau-nao', 'instagram', 'https://www.instagram.com/didaunao2022/', 'didaunao2022', 21000),
        ('hanbow-beauty', 'tiktok', 'https://www.tiktok.com/@hanbow.beauty', 'hanbow.beauty', 106200),
        ('hanbow-beauty', 'instagram', 'https://www.instagram.com/hanbow.beauty/', 'hanbow.beauty', 3800),
        ('ha-noi-an-gi', 'tiktok', 'https://www.tiktok.com/@hanoiangi', 'hanoiangi', 889000),
        ('ha-noi-an-gi', 'facebook', 'https://www.facebook.com/hanoiangiodau', 'hanoiangiodau', 221000),
        ('bui-thi-kim-luyen', 'tiktok', 'https://www.tiktok.com/@kim.luyen_laplalaplanh', 'kim.luyen_laplalaplanh', 221300),
        ('lan-cung-bean', 'tiktok', 'https://www.tiktok.com/@eatwithbeann_', 'eatwithbeann_', 109600),
        ('lan-cung-bean', 'instagram', 'https://www.instagram.com/eatwithbeann_/', 'eatwithbeann_', 30100),
        ('vy-pinky', 'tiktok', 'https://www.tiktok.com/@covynhatrang', 'covynhatrang', 140400),
        ('hazen', 'tiktok', 'https://www.tiktok.com/@hazen_ng', 'hazen_ng', 371000),
        ('hazen', 'instagram', 'https://www.instagram.com/phxthao/', 'phxthao', 64000),
        ('dung-amy', 'facebook', 'https://www.facebook.com/dung.amyybs', 'dung.amyybs', 104500),
        ('dung-amy', 'instagram', 'https://www.instagram.com/dungamy_/', 'dungamy_', 1700),
        ('nha-cua-bi', 'tiktok', 'https://www.tiktok.com/@mebireview', 'mebireview', 139000),
        ('nha-cua-bi', 'facebook', 'https://www.facebook.com/ngocanh.phamthi.18041', 'ngocanh.phamthi.18041', 52000),
        ('thu-uyen', 'tiktok', 'https://www.tiktok.com/@thuuyen1168', 'thuuyen1168', 97800),
        ('hoang-thi-phuong-thao', 'facebook', 'https://www.facebook.com/thao.hoang.547389', 'thao.hoang.547389', 208000),
        ('hoang-thi-phuong-thao', 'instagram', 'https://www.instagram.com/hoangthi_phuongthaoo/', 'hoangthi_phuongthaoo', 6600),
        ('trinh-thi-my-le', 'facebook', 'https://www.facebook.com/myle999', 'myle999', 424000),
        ('trinh-thi-my-le', 'instagram', 'https://www.instagram.com/trinhmyle/', 'trinhmyle', 40000),
        ('trinh-thi-my-le', 'tiktok', 'https://www.tiktok.com/@trinhthimyle99', 'trinhthimyle99', 103000),
        ('gia-dinh-mat-mat', 'tiktok', 'https://www.tiktok.com/@giadinhmatmat', 'giadinhmatmat', 63000),
        ('huyen-nguyen-hmeuu', 'tiktok', 'https://www.tiktok.com/@hmeuu', 'hmeuu', 470100),
        ('huyen-nguyen-hmeuu', 'facebook', 'https://www.facebook.com/Huyennguyenhmeuu', 'Huyennguyenhmeuu', 173000),
        ('huyen-nguyen-hmeuu', 'instagram', 'https://www.instagram.com/hmeuu/', 'hmeuu', 116000),
        ('tuyen-pham-family', 'tiktok', 'https://www.tiktok.com/@tuyenphamfamily', 'tuyenphamfamily', 41000),
        ('thu-nhi-eat-clean-hong', 'tiktok', 'https://www.tiktok.com/@tuilathinhu', 'tuilathinhu', 1600000),
        ('thu-nhi-eat-clean-hong', 'facebook', 'https://www.facebook.com/eatcleanhong', 'eatcleanhong', 1000000),
        ('thu-nhi-eat-clean-hong', 'instagram', 'https://www.instagram.com/tuilathunhineeeeee/', 'tuilathunhineeeeee', 28900),
        ('thu-nhi-eat-clean-hong', 'youtube', 'https://www.youtube.com/c/EatCleanHongThuNhi79', 'EatCleanHongThuNhi79', 10000),
        ('eatwhning', 'tiktok', 'https://www.tiktok.com/@eatwhning', 'eatwhning', 35900),
        ('eatwhning', 'facebook', 'https://www.facebook.com/eatwhning', 'eatwhning', 2400),
        ('eatwhning', 'instagram', 'https://www.instagram.com/eatwhning/', 'eatwhning', 1100),
        ('meily', 'tiktok', 'https://www.tiktok.com/@meimeily_', 'meimeily_', 136000),
        ('thanh-lan', 'tiktok', 'https://www.tiktok.com/@lanlan822331', 'lanlan822331', 182000),
        ('thanh-lan', 'facebook', 'https://www.facebook.com/thanhlan.hnvn', 'thanhlan.hnvn', 222000),
        ('hai-wanderlust', 'tiktok', 'https://www.tiktok.com/@food_wanderlust', 'food_wanderlust', 213500),
        ('hai-wanderlust', 'facebook', 'https://www.facebook.com/hahai.nguyen.3', 'hahai.nguyen.3', 31300),
        ('hai-wanderlust', 'instagram', 'https://www.instagram.com/hahaiday/', 'hahaiday', 111000),
        ('ng-le-thu-thuy', 'tiktok', 'https://www.tiktok.com/@nglethuthuy', 'nglethuthuy', 250400),
        ('ng-le-thu-thuy', 'instagram', 'https://www.instagram.com/nglethuthuy/', 'nglethuthuy', 8200),
        ('di-cung-thy', 'tiktok', 'https://www.tiktok.com/@dicungthy', 'dicungthy', 144000),
        ('di-cung-thy', 'facebook', 'https://www.facebook.com/dicungthy', 'dicungthy', 1100),
        ('di-cung-thy', 'youtube', 'https://www.youtube.com/dicungthy', 'dicungthy', 6700),
        ('pham-ha-phuong-thao', 'facebook', 'https://www.facebook.com/pham.haphuongthao', 'pham.haphuongthao', 225000),
        ('pham-ha-phuong-thao', 'tiktok', 'https://www.tiktok.com/@ph.phuonggthaoo13', 'ph.phuonggthaoo13', 57200),
        ('pham-ha-phuong-thao', 'instagram', 'https://www.instagram.com/ph.phuonggthaoo', 'ph.phuonggthaoo', 22500),
        ('sak-food-an-gi-choi-gi', 'facebook', 'https://www.facebook.com/sakfood', 'sakfood', 3200),
        ('sak-food-an-gi-choi-gi', 'tiktok', 'https://www.tiktok.com/@sakfood', 'sakfood', 12900),
        ('sak-food-an-gi-choi-gi', 'instagram', 'https://www.instagram.com/sak.food/', 'sak.food', 11500),
        ('sak-food-an-gi-choi-gi', 'youtube', 'https://www.youtube.com/sakfoodchannel', 'sakfoodchannel', 2600),
        ('nghia-tinon', 'tiktok', 'https://www.tiktok.com/@nghia2302', 'nghia2302', 119500),
        ('nghia-tinon', 'instagram', 'https://www.instagram.com/_nghia2302/', '_nghia2302', 10600),
        ('quin-vu-thuy-quynh', 'tiktok', 'https://www.tiktok.com/@quinskincareholic', 'quinskincareholic', 48100),
        ('quin-vu-thuy-quynh', 'facebook', 'https://www.facebook.com/quinblog', 'quinblog', 39000),
        ('quin-vu-thuy-quynh', 'instagram', 'https://www.instagram.com/quinquin1194/', 'quinquin1194', 14900),
        ('quin-vu-thuy-quynh', 'youtube', 'https://www.youtube.com/@quinquin/videos', 'quinquin', 315000),
        ('henry-tran', 'tiktok', 'https://www.tiktok.com/@henry_quynhgo', 'henry_quynhgo', 211700),
        ('henry-tran', 'facebook', 'https://www.facebook.com/vancamp.emily.9', 'vancamp.emily.9', 144800),
        ('fancy-with-trang', 'tiktok', 'https://www.tiktok.com/@fancywithtrang_', 'fancywithtrang_', 81500),
        ('fancy-with-trang', 'facebook', 'https://www.facebook.com/people/Trang-Đặng/', NULL, 10500),
        ('fancy-with-trang', 'instagram', 'https://www.instagram.com/fancywithtrang/', 'fancywithtrang', 72200),
        ('lee-hay-di', 'tiktok', 'https://www.tiktok.com/@lee.haydi', 'lee.haydi', 101000),
        ('lee-hay-di', 'facebook', 'https://www.facebook.com/lehoang.an.108', 'lehoang.an.108', 101000),
        ('lee-hay-di', 'instagram', 'https://www.instagram.com/lee.haydi', 'lee.haydi', 14500),
        ('doan-thanh-ngan', 'tiktok', 'https://www.tiktok.com/@ngan_aleybeauty', 'ngan_aleybeauty', 314100),
        ('doan-thanh-ngan', 'facebook', 'https://www.facebook.com/Leader.Team.DoanThanhNgan', 'Leader.Team.DoanThanhNgan', 21000),
        ('nguyen-thoai-nghi', 'tiktok', 'https://www.tiktok.com/@thoainghi_', 'thoainghi_', 112600),
        ('nguyen-thoai-nghi', 'facebook', 'https://www.facebook.com/NguyenThoaiNghi140905', 'NguyenThoaiNghi140905', 401700),
        ('nguyen-thoai-nghi', 'instagram', 'https://www.instagram.com/thoainghi_/', 'thoainghi_', 367000),
        ('quynh-go', 'tiktok', 'https://www.tiktok.com/@quynhgo', 'quynhgo', 224700),
        ('quynh-go', 'facebook', 'https://www.facebook.com/quynh.giao.31', 'quynh.giao.31', 125000),
        ('quynh-go', 'instagram', 'https://www.instagram.com/gy_ao/', 'gy_ao', 20000)
)
-- kol_social_channel has no UNIQUE; guard with NOT EXISTS so a rerun
-- doesn't duplicate (kol_profile_id, platform, url) rows.
INSERT INTO kol_social_channel (kol_profile_id, platform, url, username, follower_count, engagement_rate, verified)
SELECT
    ap.profile_id,
    si.platform,
    si.url,
    si.username,
    si.follower_count,
    0,
    false
FROM social_input si
JOIN all_profiles ap ON ap.slug = si.slug
WHERE si.slug IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM kol_social_channel ksc
      WHERE ksc.kol_profile_id = ap.profile_id
        AND ksc.platform       = si.platform
        AND ksc.url            = si.url
  );

COMMIT;

-- ======================================================================
-- Verify counts after running:
--   SELECT COUNT(*) FROM app_user WHERE email LIKE '%@kols-koc.imported';  -- expect 108
--   SELECT COUNT(*) FROM kol_profile WHERE status = 'DRAFT';                -- expect 108 (or more)
--   SELECT COUNT(*) FROM kol_social_channel sc
--    JOIN kol_profile p ON p.id = sc.kol_profile_id
--    JOIN app_user u ON u.id = p.user_id
--    WHERE u.email LIKE '%@kols-koc.imported';                              -- expect 273
-- ======================================================================
-- Notes:
--   * password_hash is a placeholder. Reset before letting users log in.
--   * role='KOL' / status='PENDING_VERIFICATION'. Adjust if your enums differ.
--   * follower_count parsed from text like '1.3M+', '376K+', '160N+'.
--     'N' suffix (Vietnamese 'Nghìn') is treated as thousand.
--   * engagement_rate=0 and verified=false (no source data).
--   * social_input is keyed by kol_profile.slug, which must be unique.
--   * If kol_profile.slug has no UNIQUE index, a profile with duplicate
--     slug would receive the same social rows multiple times.
-- ======================================================================
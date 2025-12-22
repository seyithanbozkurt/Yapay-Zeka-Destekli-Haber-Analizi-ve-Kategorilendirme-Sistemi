package com.bitirme.config;

import com.bitirme.entity.Category;
import com.bitirme.entity.ModelVersion;
import com.bitirme.entity.Role;
import com.bitirme.entity.Source;
import com.bitirme.entity.User;
import com.bitirme.repository.CategoryRepository;
import com.bitirme.repository.ModelVersionRepository;
import com.bitirme.repository.RoleRepository;
import com.bitirme.repository.SourceRepository;
import com.bitirme.repository.UserRepository;
import com.bitirme.service.NewsCrawlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SourceRepository sourceRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final NewsCrawlerService newsCrawlerService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Starting data initialization...");
        initializeRoles();
        initializeUsers();
        initializeCategories();
        initializeSources();
        initializeModelVersion();
        log.info("Data initialization completed!");
        
        // Uygulama başladığında ilk haber çekme işlemini başlat
        log.info("Starting initial news crawl...");
        try {
            int fetchedCount = newsCrawlerService.crawlAllSources();
            log.info("Initial news crawl completed. {} news fetched.", fetchedCount);
        } catch (Exception e) {
            log.error("Error in initial news crawl: {}", e.getMessage());
        }
    }

    private void initializeRoles() {
        log.info("Initializing roles...");
        
        // ADMIN rolü - DB'de kontrol et, yoksa ekle
        if (!roleRepository.existsByName("ADMIN")) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setDescription("Yönetici rolü - Tüm yetkilere sahip");
            roleRepository.save(adminRole);
            log.info("✅ ADMIN role added to database");
        } else {
            log.info("ℹ️  ADMIN role already exists in database");
        }
        
        // USER rolü - DB'de kontrol et, yoksa ekle
        if (!roleRepository.existsByName("USER")) {
            Role userRole = new Role();
            userRole.setName("USER");
            userRole.setDescription("Kullanıcı rolü - Temel yetkiler");
            roleRepository.save(userRole);
            log.info("✅ USER role added to database");
        } else {
            log.info("ℹ️  USER role already exists in database");
        }
        
        // EDITOR rolü - DB'de kontrol et, yoksa ekle
        if (!roleRepository.existsByName("EDITOR")) {
            Role editorRole = new Role();
            editorRole.setName("EDITOR");
            editorRole.setDescription("Editör rolü - İçerik yönetim yetkileri");
            roleRepository.save(editorRole);
            log.info("✅ EDITOR role added to database");
        } else {
            log.info("ℹ️  EDITOR role already exists in database");
        }
        
        log.info("Roles initialization completed - Total roles in DB: {}", roleRepository.count());
    }

    private void initializeUsers() {
        log.info("Initializing users...");
        
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
        
        // Metehan Admin - DB'de kontrol et, yoksa ekle
        if (!userRepository.existsByUsername("metehan")) {
            User metehan = new User();
            metehan.setUsername("metehan");
            metehan.setEmail("metehan@bitirme.com");
            metehan.setPasswordHash(passwordEncoder.encode("123123123"));
            metehan.setActive(true);
            Set<Role> metehanRoles = new HashSet<>();
            metehanRoles.add(adminRole);
            metehan.setRoles(metehanRoles);
            userRepository.save(metehan);
            log.info("Metehan admin user added to database");
        } else {
            log.info("Metehan admin user already exists in database");
        }
        
        // Seyithan Admin - DB'de kontrol et, yoksa ekle
        if (!userRepository.existsByUsername("seyithan")) {
            User seyithan = new User();
            seyithan.setUsername("seyithan");
            seyithan.setEmail("seyithan@bitirme.com");
            seyithan.setPasswordHash(passwordEncoder.encode("123123123"));
            seyithan.setActive(true);
            Set<Role> seyithanRoles = new HashSet<>();
            seyithanRoles.add(adminRole);
            seyithan.setRoles(seyithanRoles);
            userRepository.save(seyithan);
            log.info("Seyithan admin user added to database");
        } else {
            log.info("Seyithan admin user already exists in database");
        }
        
        log.info("Users initialization completed - Total users in DB: {}", userRepository.count());
    }

    private void initializeCategories() {
        log.info("Initializing categories...");
        
        // Tüm kategorileri DB'den kontrol et, eksik olanları ekle
        Map<String, Category> existingCategories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getName, c -> c, (c1, c2) -> c1));
        
        int addedCount = 0;
        
        // Spor
        if (!existingCategories.containsKey("Spor")) {
            Category category1 = new Category();
            category1.setName("Spor");
            category1.setDescription("Spor haberleri kategorisi");
            category1.setActive(true);
            categoryRepository.save(category1);
            addedCount++;
        }
        
        // Ekonomi
        if (!existingCategories.containsKey("Ekonomi")) {
            Category category2 = new Category();
            category2.setName("Ekonomi");
            category2.setDescription("Ekonomi haberleri kategorisi");
            category2.setActive(true);
            categoryRepository.save(category2);
            addedCount++;
        }
        
        // Teknoloji
        if (!existingCategories.containsKey("Teknoloji")) {
            Category category3 = new Category();
            category3.setName("Teknoloji");
            category3.setDescription("Teknoloji haberleri kategorisi");
            category3.setActive(true);
            categoryRepository.save(category3);
            addedCount++;
        }
        
        // Siyaset
        if (!existingCategories.containsKey("Siyaset")) {
            Category category4 = new Category();
            category4.setName("Siyaset");
            category4.setDescription("Siyaset haberleri kategorisi");
            category4.setActive(true);
            categoryRepository.save(category4);
            addedCount++;
        }
        
        // Sağlık
        if (!existingCategories.containsKey("Sağlık")) {
            Category category5 = new Category();
            category5.setName("Sağlık");
            category5.setDescription("Sağlık haberleri kategorisi");
            category5.setActive(true);
            categoryRepository.save(category5);
            addedCount++;
        }
        
        // Eğitim
        if (!existingCategories.containsKey("Eğitim")) {
            Category category6 = new Category();
            category6.setName("Eğitim");
            category6.setDescription("Eğitim haberleri kategorisi");
            category6.setActive(true);
            categoryRepository.save(category6);
            addedCount++;
        }
        
        // Asayiş
        if (!existingCategories.containsKey("Asayiş")) {
            Category category7 = new Category();
            category7.setName("Asayiş");
            category7.setDescription("Asayiş haberleri kategorisi");
            category7.setActive(true);
            categoryRepository.save(category7);
            addedCount++;
        }
        
        // Turizm
        if (!existingCategories.containsKey("Turizm")) {
            Category category8 = new Category();
            category8.setName("Turizm");
            category8.setDescription("Turizm haberleri kategorisi");
            category8.setActive(true);
            categoryRepository.save(category8);
            addedCount++;
        }
        
        // Doğal Afet
        if (!existingCategories.containsKey("Doğal Afet")) {
            Category category9 = new Category();
            category9.setName("Doğal Afet");
            category9.setDescription("Doğal afet haberleri kategorisi");
            category9.setActive(true);
            categoryRepository.save(category9);
            addedCount++;
        }
        
        // Ulaşım
        if (!existingCategories.containsKey("Ulaşım")) {
            Category category10 = new Category();
            category10.setName("Ulaşım");
            category10.setDescription("Ulaşım haberleri kategorisi");
            category10.setActive(true);
            categoryRepository.save(category10);
            addedCount++;
        }
        
        // Çevre
        if (!existingCategories.containsKey("Çevre")) {
            Category category11 = new Category();
            category11.setName("Çevre");
            category11.setDescription("Çevre haberleri kategorisi");
            category11.setActive(true);
            categoryRepository.save(category11);
            addedCount++;
        }
        
        // Magazin
        if (!existingCategories.containsKey("Magazin")) {
            Category category12 = new Category();
            category12.setName("Magazin");
            category12.setDescription("Magazin haberleri kategorisi");
            category12.setActive(true);
            categoryRepository.save(category12);
            addedCount++;
        }
        
        // Eksik kategorileri ekle
        if (!existingCategories.containsKey("Kültür-Sanat")) {
            Category category13 = new Category();
            category13.setName("Kültür-Sanat");
            category13.setDescription("Kültür ve sanat haberleri kategorisi");
            category13.setActive(true);
            categoryRepository.save(category13);
            addedCount++;
        }
        
        if (!existingCategories.containsKey("Bilim-Teknoloji")) {
            Category category14 = new Category();
            category14.setName("Bilim-Teknoloji");
            category14.setDescription("Bilim ve teknoloji haberleri kategorisi");
            category14.setActive(true);
            categoryRepository.save(category14);
            addedCount++;
        }
        
        if (!existingCategories.containsKey("Dünya")) {
            Category category15 = new Category();
            category15.setName("Dünya");
            category15.setDescription("Dünya haberleri kategorisi");
            category15.setActive(true);
            categoryRepository.save(category15);
            addedCount++;
        }
        
        if (!existingCategories.containsKey("Sosyal")) {
            Category category16 = new Category();
            category16.setName("Sosyal");
            category16.setDescription("Sosyal medya ve toplum haberleri kategorisi");
            category16.setActive(true);
            categoryRepository.save(category16);
            addedCount++;
        }
        
        if (!existingCategories.containsKey("Diğer")) {
            Category category17 = new Category();
            category17.setName("Diğer");
            category17.setDescription("Diğer haberler kategorisi");
            category17.setActive(true);
            categoryRepository.save(category17);
            addedCount++;
        }

        log.info("Categories initialization completed - {} categories added, total: {}", addedCount, categoryRepository.count());
    }

    private void initializeModelVersion() {
        log.info("Initializing model version...");
        
        // Model versiyonu DB'de kontrol et, yoksa ekle
        String modelVersionName = "v1.0 - Keyword Based Classifier";
        ModelVersion existing = modelVersionRepository.findAll().stream()
                .filter(mv -> mv.getName().equals(modelVersionName))
                .findFirst()
                .orElse(null);
        
        if (existing == null) {
            ModelVersion modelVersion = new ModelVersion();
            modelVersion.setName(modelVersionName);
            modelVersion.setDescription("Anahtar kelime tabanlı kategorilendirme modeli");
            
            // Admin kullanıcısını createdBy olarak ata (varsa)
            Optional<User> adminUser = userRepository.findByUsername("admin");
            if (adminUser.isPresent()) {
                modelVersion.setCreatedBy(adminUser.get());
            } else {
                // Admin yoksa metehan veya seyithan'ı kullan
                Optional<User> metehanUser = userRepository.findByUsername("metehan");
                if (metehanUser.isPresent()) {
                    modelVersion.setCreatedBy(metehanUser.get());
                } else {
                    Optional<User> seyithanUser = userRepository.findByUsername("seyithan");
                    seyithanUser.ifPresent(modelVersion::setCreatedBy);
                }
            }
            
            modelVersionRepository.save(modelVersion);
            log.info("✅ Model version added to database");
        } else {
            log.info("ℹ️  Model version already exists in database");
        }
        
        log.info("Model version initialization completed - Total model versions in DB: {}", modelVersionRepository.count());
    }

    private void initializeSources() {
        log.info("Initializing sources...");
        
        // Tüm kaynakları DB'den kontrol et, eksik olanları ekle
        java.util.Map<String, Source> existingSources = sourceRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(s -> s.getName(), s -> s, (s1, s2) -> s1));
        
        int addedCount = 0;
        
        // 1. Hürriyet - Son Dakika
        if (!existingSources.containsKey("Hürriyet")) {
            Source source1 = createSource("Hürriyet", "https://www.hurriyet.com.tr", 
                    "https://www.hurriyet.com.tr/gundem/", "/gundem", 
                    "a[href*='/gundem/']", "h2", "p", "breaking_news",
                    "https://www.hurriyet.com.tr/son-dakika");
            sourceRepository.save(source1);
            addedCount++;
        } else {
            // Mevcut kaydı güncelle
            Source existingHurriyet = existingSources.get("Hürriyet");
            if (existingHurriyet.getCrawlUrl() == null || existingHurriyet.getCrawlUrl().isEmpty()) {
                existingHurriyet.setCrawlUrl("https://www.hurriyet.com.tr/gundem/");
                existingHurriyet.setLinkSelector("a[href*='/gundem/']");
                existingHurriyet.setTitleSelector("h2");
                existingHurriyet.setContentSelector("p");
                sourceRepository.save(existingHurriyet);
            }
        }

        // 2. Sabah
        if (!existingSources.containsKey("Sabah")) {
            Source source2 = createSource("Sabah", "https://www.sabah.com.tr",
                    "https://www.sabah.com.tr/gundem", "/gundem",
                    "a.news-card-link, a[href*='/gundem/']", "h2, h3", "p", "general", null);
            sourceRepository.save(source2);
            addedCount++;
        } else {
            // Mevcut kaydı güncelle
            Source existingSabah = existingSources.get("Sabah");
            existingSabah.setCrawlUrl("https://www.sabah.com.tr/gundem");
            existingSabah.setLinkSelector("a.news-card-link, a[href*='/gundem/']");
            existingSabah.setTitleSelector("h2, h3");
            existingSabah.setContentSelector("p");
            sourceRepository.save(existingSabah);
        }

        // 3. Milliyet
        if (!existingSources.containsKey("Milliyet")) {
            Source source3 = createSource("Milliyet", "https://www.milliyet.com.tr",
                    "https://www.milliyet.com.tr/gundem/", "/gundem",
                    "strong.cat-list-card__title a, h3.cat-list-card__title a, a[href*='/gundem/']", 
                    "strong.cat-list-card__title, h3.cat-list-card__title", "p", "general", null);
            sourceRepository.save(source3);
            addedCount++;
        } else {
            // Mevcut kaydı güncelle
            Source existingMilliyet = existingSources.get("Milliyet");
            if (existingMilliyet.getCrawlUrl() == null || existingMilliyet.getCrawlUrl().isEmpty()) {
                existingMilliyet.setCrawlUrl("https://www.milliyet.com.tr/gundem/");
                existingMilliyet.setLinkSelector("strong.cat-list-card__title a, h3.cat-list-card__title a, a[href*='/gundem/']");
                existingMilliyet.setTitleSelector("strong.cat-list-card__title, h3.cat-list-card__title");
                existingMilliyet.setContentSelector("p");
                sourceRepository.save(existingMilliyet);
            }
        }

        // 4. CNN Türk - Son Dakika
        if (!existingSources.containsKey("CNN Türk")) {
            Source source4 = createSource("CNN Türk", "https://www.cnnturk.com",
                    "https://www.cnnturk.com/turkiye", "/turkiye",
                    "a[href*='/turkiye/']", "h2, h3", "p", "breaking_news",
                    "https://www.cnnturk.com/son-dakika");
            sourceRepository.save(source4);
            addedCount++;
        } else {
            // Mevcut kaydı güncelle
            Source existingCnnTurk = existingSources.get("CNN Türk");
            existingCnnTurk.setCrawlUrl("https://www.cnnturk.com/turkiye");
            existingCnnTurk.setLinkSelector("a[href*='/turkiye/']");
            existingCnnTurk.setTitleSelector("h2, h3");
            existingCnnTurk.setContentSelector("p");
            sourceRepository.save(existingCnnTurk);
        }

        // 5. NTV
        if (!existingSources.containsKey("NTV")) {
            Source source5 = createSource("NTV", "https://www.ntv.com.tr",
                    "https://www.ntv.com.tr/turkiye", "/turkiye",
                    "a[href*='/turkiye/']", "h2, h3", null, "general", null);
            sourceRepository.save(source5);
            addedCount++;
        }

        // 6. Sözcü
        if (!existingSources.containsKey("Sözcü")) {
            Source source6 = createSource("Sözcü", "https://www.sozcu.com.tr",
                    "https://www.sozcu.com.tr/kategori/gundem/", "/gundem",
                    "a[href*='/gundem/']", "h2, h3", null, "general", null);
            sourceRepository.save(source6);
            addedCount++;
        }

        // 7. Habertürk
        if (!existingSources.containsKey("Habertürk")) {
            Source source7 = createSource("Habertürk", "https://www.haberturk.com",
                    "https://www.haberturk.com/gundem", "/gundem",
                    "a[href*='/gundem/']", "h2, h3, h4", "p", "general", null);
            sourceRepository.save(source7);
            addedCount++;
        }

        // 8. TRT Haber
        if (!existingSources.containsKey("TRT Haber")) {
            Source source8 = createSource("TRT Haber", "https://www.trthaber.com",
                    "https://www.trthaber.com/haber/gundem", "/haber/gundem",
                    "a[href*='/haber/gundem/'], a[href*='/haber/']", "h2, h3, h4", "p", "general", null);
            sourceRepository.save(source8);
            addedCount++;
        } else {
            // Mevcut kaydı güncelle
            Source existingTrthaber = existingSources.get("TRT Haber");
            existingTrthaber.setCrawlUrl("https://www.trthaber.com/haber/gundem");
            existingTrthaber.setLinkSelector("a[href*='/haber/gundem/'], a[href*='/haber/']");
            existingTrthaber.setTitleSelector("h2, h3, h4");
            existingTrthaber.setContentSelector("p");
            sourceRepository.save(existingTrthaber);
        }

        // 9. Anadolu Ajansı
        if (!existingSources.containsKey("Anadolu Ajansı")) {
            Source source9 = createSource("Anadolu Ajansı", "https://www.aa.com.tr",
                    "https://www.aa.com.tr/tr/gundem", "/gundem",
                    "a[href*='/tr/gundem/']", "h2, h3", "p", "breaking_news",
                    "https://www.aa.com.tr/tr/gundem");
            sourceRepository.save(source9);
            addedCount++;
        }

        // 10. Yeni Şafak
        if (!existingSources.containsKey("Yeni Şafak")) {
            Source source10 = createSource("Yeni Şafak", "https://www.yenisafak.com",
                    "https://www.yenisafak.com/gundem", "/gundem",
                    "a[href*='/gundem/']", "h2, h3", "p", "general", null);
            sourceRepository.save(source10);
            addedCount++;
        }

        // 11. Takvim
        if (!existingSources.containsKey("Takvim")) {
            Source source11 = createSource("Takvim", "https://www.takvim.com.tr",
                    "https://www.takvim.com.tr/guncel", "/guncel",
                    "a[href*='/guncel/']", "h2, h3", "p", "general", null);
            sourceRepository.save(source11);
            addedCount++;
        }

        // 12. Star
        if (!existingSources.containsKey("Star")) {
            Source source12 = createSource("Star", "https://www.star.com.tr",
                    "https://www.star.com.tr/gundem", "/gundem",
                    "a[href*='/gundem/']", "h2, h3", "p", "general", null);
            sourceRepository.save(source12);
            addedCount++;
        }

        log.info("Sources initialization completed - {} sources added, total: {}", addedCount, sourceRepository.count());
    }

    private Source createSource(String name, String baseUrl, String crawlUrl, String categoryPath,
                               String linkSelector, String titleSelector, String contentSelector,
                               String crawlType, String lastMinuteUrl) {
        Source source = new Source();
        source.setName(name);
        source.setBaseUrl(baseUrl);
        source.setCrawlUrl(crawlUrl);
        source.setCategoryPath(categoryPath);
        source.setLinkSelector(linkSelector);
        source.setTitleSelector(titleSelector);
        source.setContentSelector(contentSelector);
        source.setCrawlType(crawlType);
        source.setLastMinuteUrl(lastMinuteUrl);
        source.setActive(true);
        return source;
    }
}


# ZarFlow -- production deploy (1 GB EC2)

## 1. AWS checklist (bir martalik)

- **Elastic IP** ajratib, instansega biriktiring (IP o'zgarmasligi uchun).
- **Security Group**: 22 -- faqat sizning IP, 80 va 443 -- hammaga (0.0.0.0/0).
- **Termination protection** yoqilgan, **stop protection** yoqilgan.
- **EBS**: gp3, 30 GB.
- **CPU credits**: "standard" (unlimited emas -- kutilmagan to'lovlarning oldini oladi).
- **Billing budget alert** qo'ying (masalan $5/$10 bo'sag'ida email).

## 2. Serverda buyruqlar -- aniq shu tartibda

```bash
# 1) Reponi vaqtinchalik ubuntu user bilan klon qiling (faqat setup skriptini olish uchun)
ssh ubuntu@<server-ip>
git clone https://github.com/<owner>/zarflow.git /tmp/zarflow
sudo /tmp/zarflow/scripts/server-setup.sh
```

Skript oxirida bosiladigan **ed25519 public key** chiqadi -- uni GitHub repo
`Settings -> Deploy keys -> Add deploy key` ga **READ-ONLY** (Allow write access
belgisini QO'YMANG) sifatida qo'shing.

```bash
# 2) zarflow user sifatida kiring (parol emas, xuddi shu SSH kalitingiz ishlaydi)
ssh zarflow@<server-ip>

# 3) asosiy reponi klon qiling (deploy key orqali, read-only)
git clone git@github.com:<owner>/zarflow.git ~/zarflow
cd ~/zarflow

# 4) .env.prod to'ldiring
cp .env.prod.example .env.prod
nano .env.prod          # barcha placeholder qiymatlarni to'ldiring
chmod 600 .env.prod

# 5) GHCR'dan image tortish uchun login (read:packages huquqli GitHub token bilan)
echo <GHCR_TOKEN> | docker login ghcr.io -u <github-username> --password-stdin

# 6) birinchi deploy
scripts/deploy.sh

# 7) kunlik backup uchun cron qo'shing (scripts/backup.sh chiqargan qatorlarni ishlating)
crontab -e

# 8) hammasi ishlayotganiga ishonch hosil qilgandan keyin, SSH'ni qattiqlashtiring
exit                     # qayta ubuntu/root sifatida kiring
sudo /tmp/zarflow/scripts/harden-ssh.sh
```

## 3. Kundalik ishlar

**Loglarni ko'rish:**
```bash
cd ~/zarflow
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f web
```

**Xotira holatini tekshirish:**
```bash
docker stats --no-stream
free -h
```

**Rollback** (oldingi git sha'ga qaytish):
```bash
scripts/deploy.sh <git-sha>
```

**Backup'dan tiklash:**
```bash
scripts/restore.sh ~/backups/zarflow-20260101-0300.sql.gz
```

**sslip.io'dan haqiqiy domenga o'tish:**
1. Domenning A-yozuvini (A record) Elastic IP'ga ko'rsating.
2. `.env.prod` faylida `ZARFLOW_DOMAIN` ni yangi domenga almashtiring.
3. `TELEGRAM_WEBHOOK_URL` va `ZARFLOW_MINI_APP_URL` ni ham yangilang.
4. `scripts/deploy.sh` ni qayta ishga tushiring -- Caddy yangi domen uchun
   avtomatik ravishda yangi sertifikat oladi.

## 4. Agar server sekinlashsa

Instansani to'xtating, turini **t3.small**ga o'zgartiring, qayta ishga tushiring
-- boshqa hech narsani o'zgartirish shart emas (image'lar, compose fayl,
volume'lar -- hammasi joyida qoladi).

## 5a. Oracle Cloud (ARM/aarch64, Always Free) uchun farqlar

Yuqoridagi barcha qadamlar bir xil ishlaydi -- image'lar endi `linux/amd64` va
`linux/arm64` uchun multi-arch qilib chiqariladi, shuning uchun `docker compose
pull` o'zi to'g'ri arxitekturani tanlaydi. Farq qiladigan joylar:

- **Elastic IP o'rniga**: Oracle'da instansega biriktirilgan **public IP
  o'zgarmaydi** (reserved IP talab qilinmaydi), lekin uni Networking ->
  IP Management'dan tasdiqlang.
- **sslip.io domen**: alohida domen kerak emas -- public IP'ni
  `<ip-tireshlar-bilan>.sslip.io` ko'rinishiga o'tkazing (masalan
  `79-76-117-34.sslip.io`). `.env.prod`'da `ZARFLOW_DOMAIN` shu qiymat bo'ladi.
- **Firewall -- faqat VCN Security List orqali**: `scripts/server-setup.sh`
  Oracle'ni avtomatik aniqlab, **ufw'ni yoqmaydi**. OCI'da ikki qavatli filtr
  bor (VCN security list + instansdagi tayyor iptables qoidalari); ufw'ni
  ustiga yoqish odatiy "o'zingni tashqariga qulflash" xatosi. 22/80/443
  portlarini **Networking -> Virtual Cloud Network -> Security Lists** orqali
  oching, ufw emas.
- **CPU/RAM**: Always Free ARM shape (`VM.Standard.A1.Flex`) 1-4 OCPU / 6-24 GB
  oralig'ida tanlanadi -- 1 OCPU / 6 GB ham bu stack uchun yetarli.

## 5. Ogohlantirish

**AWS Free Plan 6 oydan keyin yoki kredit tugaganda akkauntni avtomatik
yopadi.** Shuning uchun:
- Backup'larni doim **serverdan tashqarida** ham saqlang (`scripts/backup.sh`
  chiqargan `rsync` buyrug'ini laptopingizda kunlik ishga tushiring).
- 5-oyga yetmasdan oldin **Paid plan**ga o'tish yoki boshqa providerga
  ko'chirish haqida qaror qabul qiling.

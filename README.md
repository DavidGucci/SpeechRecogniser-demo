# ToDoMap

ToDoMap je Android aplikacija, ki združuje upravljanje opravil (to-do) z lokacijskimi funkcijami.  
Uporabnikom omogoča ustvarjanje opravil, ki so vezana na določeno lokacijo, ter prejemanje samodejnih obvestil (push notifications), ko se približajo izbrani točki. Aplikacija uporablja geofencing tehnologijo za pametno in kontekstualno obveščanje.

---

## Namen aplikacije

Namen aplikacije je uporabniku olajšati upravljanje vsakodnevnih opravil tako, da se ta samodejno sprožijo glede na uporabnikovo lokacijo.  
Primeri uporabe:
- Opomnik za nakup določenih izdelkov ob prihodu v trgovino.
- Opomnik za obisk določenega kraja ali osebe.
- Lokacijski opomniki za opravke med potovanjem.
- Ustvarjanje “geo-opravil”, ki se aktivirajo v radiju določene razdalje.

---

## Avtor
**David Gucci**  
FERI RIT UN

---

## Uporabljene tehnologije

Aplikacija temelji na sodobnih Android tehnologijah in knjižnicah:

### **Android / Kotlin**
- **Kotlin** – glavni programski jezik aplikacije  
- **Android SDK (API 24+)**

### **Zemljevidi in lokacija**
- **Google Maps SDK for Android** – prikaz zemljevida  
- **Google Play Services – Location** – pridobivanje lokacije  
- **Geofencing API** – sprožanje dogodkov ob vstopu/izstopu iz območja  

### **Uporabniški vmesnik**
- **XML**
- **Material Design**

### **Push Notifications**
- **Firebase Cloud Messaging (FCM)** – pošiljanje in prejemanje potisnih obvestil  
- **Android Notification Manager**

---

## Ključne funkcionalnosti (povzetek)

- Dodajanje opravil z imenom, opisom in lokacijo.  
- Prikaz nalog na zemljevidu Google Maps.  
- Geofencing opomniki – obvestilo, ko se uporabnik približa določeni nalogi.  
- Push notifications s sistemskimi obvestili.  
- Lokalna hramba podatkov za delo brez interneta.  
- Urejen in intuitiven uporabniški vmesnik.  

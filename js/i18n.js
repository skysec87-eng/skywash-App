/* skyWash i18n — client-side dictionaries (no API required). */
(function (global) {
  const LOCALES = {
    en: {
      name: 'English',
      strings: {
        'nav.book': 'Book',
        'nav.map': 'Map',
        'nav.orders': 'Orders',
        'brand.tag': 'On-demand cleaning',
        'ob.kicker': 'Fresh laundry, on demand',
        'ob.tagline': 'Book a pickup in minutes — partners come to you.',
        'ob.create': 'Create account',
        'ob.login': 'Log in',
        'ob.or': 'or',
        'ob.forgot': 'Forgot access? Reset with email',
        'ob.emailTitle': 'Create your account',
        'ob.emailCopy': 'Enter your email — we’ll send a one-time code. No password needed.',
        'ob.email': 'Email',
        'ob.continue': 'Continue',
        'ob.codeTitle': 'Enter the code',
        'ob.codeCopy': 'We sent a 6-digit code to your email.',
        'ob.code': 'Verification code',
        'ob.resend': 'Resend code',
        'ob.verify': 'Verify',
        'ob.almost': 'Almost there',
        'ob.profileCopy': 'Add your name and phone so partners can reach you at pickup.',
        'ob.name': 'Name',
        'ob.phone': 'Phone',
        'ob.start': 'Start booking',
        'book.title': 'What are we washing today?',
        'book.sub': 'Get matched with the nearest laundry partner, like ordering a ride.',
        'book.now': 'Pickup now',
        'book.later': 'Schedule for later',
        'book.location': 'Pickup location',
        'book.addrPh': 'Enter your pickup address',
        'book.locSet': '✓ Location set — matching nearby partners',
        'book.services': 'Service type',
        'book.weight': 'Estimated weight',
        'book.payment': 'Payment method',
        'book.promo': 'Promo code',
        'book.apply': 'Apply',
        'book.request': 'Set a pickup location first',
        'nav.bookDesktop': 'Book pickup',
        'nav.mapDesktop': 'Browse map',
        'nav.ordersDesktop': 'My orders',
        'pay.card': 'Debit / Credit Card',
        'pay.transfer': 'Bank Transfer',
        'pay.cash': 'Cash on Pickup',
        'match.title': 'Choose a partner',
        'match.sub': 'Nearby laundries — pick who you want, then confirm.',
        'match.eta': 'Estimated pickup time',
        'match.total': 'Total to pay',
        'match.confirm': 'Confirm pickup',
        'match.selectFirst': 'Select a partner first',
        'match.cancel': 'Cancel',
        'trip.status': 'Status',
        'trip.eta': 'ETA',
        'trip.assist': 'skyWash Assist',
        'trip.contactSupport': 'Contact support',
        'trip.supportEmail': 'Email support',
        'trip.pickupEta': 'pickup',
        'trip.washEta': 'wash',
        'trip.deliveryEta': 'delivery',
        'trip.confirmDelivery': 'Confirm receipt — no issues',
        'trip.confirmDeliveryHint': 'Tap when you’ve received your laundry and everything looks good. This notifies the laundry.',
        'trip.cancel': 'Cancel order',
        'rating.title': 'Delivered!',
        'rating.again': 'Book another pickup',
        'browse.title': 'All laundry partners',
        'browse.sub': 'Browse every provider on the map.',
        'browse.search': 'Search by name, area, or city…',
        'orders.title': 'My orders',
        'orders.sub': 'Your past pickups with skyWash Cleaning.',
        'acct.title': 'Account',
        'acct.sub': 'Your profile, appearance, and language.',
        'acct.name': 'Name',
        'acct.phone': 'Phone',
        'acct.email': 'Email',
        'acct.payment': 'Default payment',
        'acct.save': 'Save changes',
        'acct.saved': 'Profile saved.',
        'acct.reset': 'Reset access (new login code)',
        'acct.logout': 'Log out',
        'acct.logoutAll': 'Log out everywhere',
        'prefs.appearance': 'Appearance',
        'prefs.appearanceHint': 'Choose light, dark, or follow your device.',
        'prefs.light': 'Light',
        'prefs.dark': 'Dark',
        'prefs.system': 'System',
        'prefs.language': 'Language',
        'prefs.languageHint': 'Detected from your country on first visit — change anytime here.',
        'live.default': 'Live',
        'menu.profile': 'Profile',
        'menu.reset': 'Reset access',
        'menu.logout': 'Log out',
        'menu.logoutAll': 'Log out everywhere'
      }
    },
    yo: {
      name: 'Yorùbá',
      strings: {
        'nav.book': 'Pàdé',
        'nav.map': 'Máàpù',
        'nav.orders': 'Àwọn ìbéèrè',
        'brand.tag': 'Ìfọ̀ṣọ àìsíwọ̀n',
        'ob.kicker': 'Aṣọ mímọ́, lẹ́sẹ̀kẹsẹ̀',
        'ob.tagline': 'Ṣe ìpàdé ní ìṣẹ́jú díẹ̀ — àwọn alábàáṣiṣẹ́ yóò wá sí ọ̀dọ̀ rẹ.',
        'ob.create': 'Ṣẹ̀dá àkáǹtì',
        'ob.login': 'Wọlé',
        'ob.or': 'tàbí',
        'ob.forgot': 'Gbàgbé ìráàyè? Ṣe àtúnṣe pẹ̀lú ímeèlì',
        'ob.emailTitle': 'Ṣẹ̀dá àkáǹtì rẹ',
        'ob.emailCopy': 'Tẹ ímeèlì rẹ — a ó fi kóòdù ránṣẹ́. Kò nílò ọ̀rọ̀ ìgbaniwọlé.',
        'ob.email': 'Ímeèlì',
        'ob.continue': 'Tẹ̀síwájú',
        'ob.codeTitle': 'Tẹ kóòdù náà',
        'ob.codeCopy': 'A ti fi kóòdù ọnà mẹ́fà ránṣẹ́ sí ímeèlì rẹ.',
        'ob.code': 'Kóòdù ìjẹ́rìísí',
        'ob.resend': 'Tún fi ránṣẹ́',
        'ob.verify': 'Ṣàyẹ̀wò',
        'ob.almost': 'Ó fẹ́rẹ̀ẹ́ tán',
        'ob.profileCopy': 'Fi orúkọ àti fóònù rẹ kún un kí wọ́n lè káàbọ̀ sí ọ ní ìgbà ìgbà.',
        'ob.name': 'Orúkọ',
        'ob.phone': 'Fóònù',
        'ob.start': 'Bẹ̀rẹ̀ ìpàdé',
        'book.title': 'Ṣe ìpàdé',
        'book.sub': 'Sọ ibi tí o wà àti ohun tí o fẹ́ fọ̀.',
        'book.when': 'Nígbà wo',
        'book.asap': 'Nísinyìí',
        'book.schedule': 'Ṣètò',
        'book.location': 'Ibi ìgbà',
        'book.locate': 'Lo ibi mi',
        'book.services': 'Àwọn iṣẹ́',
        'book.quantity': 'Iye',
        'book.payment': 'Ìsanwó',
        'book.promo': 'Kóòdù ìdínkù',
        'book.apply': 'Lo',
        'book.find': 'Wá àwọn alábàáṣiṣẹ́ nítòsí',
        'pay.card': 'Káàdì',
        'pay.transfer': 'Ìṣípò owó',
        'pay.cash': 'Owó lọ́wọ́ ní ìgbà',
        'match.title': 'Yan alábàáṣiṣẹ́',
        'match.sub': 'Àwọn ilé-iṣẹ́ nítòsí — yan ẹni tí o fẹ́, lẹ́yìn náà jẹ́rìí.',
        'match.eta': 'Àkókò ìgbà',
        'match.total': 'Àpapọ̀ ìsanwó',
        'match.confirm': 'Jẹ́rìí ìgbà',
        'match.selectFirst': 'Yan alábàáṣiṣẹ́ kọ́kọ́',
        'match.cancel': 'Fagilé',
        'trip.status': 'Ipò',
        'trip.eta': 'Àkókò',
        'trip.assist': 'Olùrànlọ́wọ́ skyWash',
        'trip.contactSupport': 'Kàn sí ìrànlọ́wọ́',
        'trip.supportEmail': 'Fi email sí ìrànlọ́wọ́',
        'trip.cancel': 'Fagilé ìbéèrè',
        'rating.title': 'A ti fi jí!',
        'rating.sub': 'Aṣọ rẹ ti mọ́. Báwo ni {name} ṣe rí?',
        'rating.again': 'Ṣe ìpàdé mìíràn',
        'browse.title': 'Gbogbo alábàáṣiṣẹ́',
        'browse.sub': 'Wo gbogbo wọn lórí máàpù.',
        'browse.search': 'Wá nípa orúkọ, àgbègbè, tàbí ìlú…',
        'orders.title': 'Àwọn ìbéèrè mi',
        'orders.sub': 'Àwọn ìgbà tẹ́lẹ̀ pẹ̀lú skyWash.',
        'acct.title': 'Àkáǹtì',
        'acct.sub': 'Ìṣètò ìdílé àti ìwọlé.',
        'acct.name': 'Orúkọ',
        'acct.phone': 'Fóònù',
        'acct.email': 'Ímeèlì',
        'acct.payment': 'Ìsanwó àìyípadà',
        'acct.save': 'Fi pamọ́',
        'acct.saved': 'A ti fi pamọ́.',
        'acct.reset': 'Ṣe àtúnṣe ìráàyè',
        'acct.logout': 'Jáde',
        'acct.logoutAll': 'Jáde nígbà gbogbo',
        'prefs.appearance': 'Ìrísí',
        'prefs.appearanceHint': 'Yan ìmọ́lẹ̀, òkùnkùn, tàbí tẹ̀lé ẹ̀rọ rẹ.',
        'prefs.light': 'Ìmọ́lẹ̀',
        'prefs.dark': 'Òkùnkùn',
        'prefs.system': 'Ẹ̀rọ',
        'prefs.language': 'Èdè',
        'prefs.languageHint': 'Ọ̀rọ̀ áyàá yí padà lórí ẹ̀rọ yìí.',
        'live.default': 'Lọ́wọ́lọ́wọ́',
        'menu.profile': 'Ìdílé',
        'menu.reset': 'Ṣe àtúnṣe ìráàyè',
        'menu.logout': 'Jáde',
        'menu.logoutAll': 'Jáde nígbà gbogbo'
      }
    },
    ha: {
      name: 'Hausa',
      strings: {
        'nav.book': 'Yi oda',
        'nav.map': 'Taswira',
        'nav.orders': 'Ododi',
        'brand.tag': 'Wanke nan-da-nan',
        'ob.kicker': 'Tufafi masu tsafta, nan da nan',
        'ob.tagline': 'Yi oda cikin mintuna — abokan hulɗa za su zo gare ka.',
        'ob.create': 'Kirkiro asusu',
        'ob.login': 'Shiga',
        'ob.or': 'ko',
        'ob.forgot': 'Ka manta shiga? Sake saita da imel',
        'ob.emailTitle': 'Kirkiro asusunka',
        'ob.emailCopy': 'Shigar da imel ɗinka — za mu aiko da lambar sirri. Babu bukatar kalmar sirri.',
        'ob.email': 'Imel',
        'ob.continue': 'Ci gaba',
        'ob.codeTitle': 'Shigar da lambar',
        'ob.codeCopy': 'Mun aiko da lambar sirri mai lambobi 6 zuwa imel ɗinka.',
        'ob.code': 'Lambar tabbatarwa',
        'ob.resend': 'Sake aikawa',
        'ob.verify': 'Tabbatar',
        'ob.almost': 'Kusan kammala',
        'ob.profileCopy': 'Ƙara suna da waya don abokan hulɗa su iya tuntuɓar ka.',
        'ob.name': 'Suna',
        'ob.phone': 'Waya',
        'ob.start': 'Fara oda',
        'book.title': 'Yi oda ɗaukar tufafi',
        'book.sub': 'Gaya mana inda kake da abin da kake son a wanke.',
        'book.when': 'Yaushe',
        'book.asap': 'Yanzu',
        'book.schedule': 'Tsara',
        'book.location': 'Wurin ɗauka',
        'book.locate': 'Yi amfani da wurina',
        'book.services': 'Ayyuka',
        'book.quantity': 'Adadi',
        'book.payment': 'Biya',
        'book.promo': 'Lambar rangwame',
        'book.apply': 'Yi amfani',
        'book.find': 'Nemo abokan hulɗa kusa',
        'pay.card': 'Kati',
        'pay.transfer': 'Canja wuri',
        'pay.cash': 'Tsabar kudi a ɗauka',
        'match.title': 'Zaɓi abokin hulɗa',
        'match.sub': 'Wuraren wanki kusa — zaɓi wanda kake so, sannan ka tabbatar.',
        'match.eta': 'Lokacin ɗauka',
        'match.total': 'Jimlar biya',
        'match.confirm': 'Tabbatar da ɗauka',
        'match.selectFirst': 'Zaɓi abokin hulɗa da farko',
        'match.cancel': 'Soke',
        'trip.status': 'Matsayi',
        'trip.eta': 'Lokaci',
        'trip.assist': 'Taimakon skyWash',
        'trip.contactSupport': 'Tuntuƙar tallafi',
        'trip.supportEmail': 'Imel tallafi',
        'trip.cancel': 'Soke oda',
        'rating.title': 'An isar!',
        'rating.sub': 'Tufafinka sun yi tsafta. Yaya {name} ya yi?',
        'rating.again': 'Yi wani oda',
        'browse.title': 'Duk abokan hulɗa',
        'browse.sub': 'Duba kowa a kan taswira.',
        'browse.search': 'Nemo da suna, unguwa, ko birni…',
        'orders.title': 'Ododi na',
        'orders.sub': 'Ododin da ka yi da skyWash.',
        'acct.title': 'Asusu',
        'acct.sub': 'Bayanan ka da saitunan shiga.',
        'acct.name': 'Suna',
        'acct.phone': 'Waya',
        'acct.email': 'Imel',
        'acct.payment': 'Hanyar biya ta asali',
        'acct.save': 'Ajiye',
        'acct.saved': 'An ajiye.',
        'acct.reset': 'Sake saita shiga',
        'acct.logout': 'Fita',
        'acct.logoutAll': 'Fita daga ko’ina',
        'prefs.appearance': 'Bayyanar',
        'prefs.appearanceHint': 'Zaɓi haske, duhu, ko bi na’urarka.',
        'prefs.light': 'Haske',
        'prefs.dark': 'Duhu',
        'prefs.system': 'Na’ura',
        'prefs.language': 'Harshe',
        'prefs.languageHint': 'Rubutu yana canzawa nan take a wannan na’ura.',
        'live.default': 'Kai tsaye',
        'menu.profile': 'Bayani',
        'menu.reset': 'Sake saita shiga',
        'menu.logout': 'Fita',
        'menu.logoutAll': 'Fita daga ko’ina'
      }
    },
    ig: {
      name: 'Igbo',
      strings: {
        'nav.book': 'Debe oda',
        'nav.map': 'Maapụ',
        'nav.orders': 'Oda m',
        'brand.tag': 'Ịsa ákwà ozugbo',
        'ob.kicker': 'Ákwà dị ọcha, ozugbo',
        'ob.tagline': 'Debe oda n’ime nkeji — ndị mmekọ ga-abịa n’ebe ị nọ.',
        'ob.create': 'Mepụta akaụntụ',
        'ob.login': 'Banye',
        'ob.or': 'ma ọ bụ',
        'ob.forgot': 'Chefuru ịbanye? Hazie ọzọ site na email',
        'ob.emailTitle': 'Mepụta akaụntụ gị',
        'ob.emailCopy': 'Tinye email gị — anyị ga-eziga koodu. Ọ dịghị mkpa paswọọdụ.',
        'ob.email': 'Email',
        'ob.continue': 'Gaa n’ihu',
        'ob.codeTitle': 'Tinye koodu',
        'ob.codeCopy': 'Anyị ezipụla koodu ọnụọgụ isii na email gị.',
        'ob.code': 'Koodu nkwenye',
        'ob.resend': 'Zighachi ọzọ',
        'ob.verify': 'Kwenye',
        'ob.almost': 'Ọ fọrọ nke nta',
        'ob.profileCopy': 'Tinye aha na ekwentị ka ndị mmekọ nwee ike ịkpọtụrụ gị.',
        'ob.name': 'Aha',
        'ob.phone': 'Ekwentị',
        'ob.start': 'Malite oda',
        'book.title': 'Debe oda mbupu',
        'book.sub': 'Gwa anyị ebe ị nọ na ihe ịchọrọ ka a saa.',
        'book.when': 'Mgbe',
        'book.asap': 'Ugbu a',
        'book.schedule': 'Hazie',
        'book.location': 'Ebe mbupu',
        'book.locate': 'Jiri ebe m nọ',
        'book.services': 'Ọrụ',
        'book.quantity': 'Ọnụọgụ',
        'book.payment': 'Ịkwụ ụgwọ',
        'book.promo': 'Koodu mbelata',
        'book.apply': 'Tinye',
        'book.find': 'Chọta ndị mmekọ dị nso',
        'pay.card': 'Kaadị',
        'pay.transfer': 'Nyefee ego',
        'pay.cash': 'Ego n’aka n’oge mbupu',
        'match.title': 'Họrọ onye mmekọ',
        'match.sub': 'Ụlọ ịsa ákwà dị nso — họrọ onye ịchọrọ, wee kwenye.',
        'match.eta': 'Oge mbupu',
        'match.total': 'Mkpokọta ụgwọ',
        'match.confirm': 'Kwenye mbupu',
        'match.selectFirst': 'Họrọ onye mmekọ mbụ',
        'match.cancel': 'Kagbuo',
        'trip.status': 'Ọnọdụ',
        'trip.eta': 'Oge',
        'trip.assist': 'Enyemaka skyWash',
        'trip.contactSupport': 'Kpọtụrụ nkwado',
        'trip.supportEmail': 'Email nkwado',
        'trip.cancel': 'Kagbuo oda',
        'rating.title': 'Ebugaala!',
        'rating.sub': 'Ákwà gị dị ọcha. Kedu ka {name} si dị?',
        'rating.again': 'Debe oda ọzọ',
        'browse.title': 'Ndị mmekọ niile',
        'browse.sub': 'Lelee ha niile na maapụ.',
        'browse.search': 'Chọọ site n’aha, mpaghara, ma ọ bụ obodo…',
        'orders.title': 'Oda m',
        'orders.sub': 'Oda gara aga gị na skyWash.',
        'acct.title': 'Akaụntụ',
        'acct.sub': 'Profaịlụ gị na ntọala nbanye.',
        'acct.name': 'Aha',
        'acct.phone': 'Ekwentị',
        'acct.email': 'Email',
        'acct.payment': 'Ụzọ ịkwụ ụgwọ ndabara',
        'acct.save': 'Chekwaa',
        'acct.saved': 'Echekwala.',
        'acct.reset': 'Hazie nbanye ọzọ',
        'acct.logout': 'Pụọ',
        'acct.logoutAll': 'Pụọ ebe niile',
        'prefs.appearance': 'Ọdịdị',
        'prefs.appearanceHint': 'Họrọ ìhè, ọchịchịrị, ma ọ bụ soro ngwaọrụ gị.',
        'prefs.light': 'Ìhè',
        'prefs.dark': 'Ọchịchịrị',
        'prefs.system': 'Ngwaọrụ',
        'prefs.language': 'Asụsụ',
        'prefs.languageHint': 'Ederede na-agbanwe ozugbo na ngwaọrụ a.',
        'live.default': 'Ugbu a',
        'menu.profile': 'Profaịlụ',
        'menu.reset': 'Hazie nbanye',
        'menu.logout': 'Pụọ',
        'menu.logoutAll': 'Pụọ ebe niile'
      }
    },
    fr: {
      name: 'Français',
      strings: {
        'nav.book': 'Réserver',
        'nav.map': 'Carte',
        'nav.orders': 'Commandes',
        'nav.bookDesktop': 'Réserver un ramassage',
        'nav.mapDesktop': 'Voir la carte',
        'nav.ordersDesktop': 'Mes commandes',
        'brand.tag': 'Nettoyage à la demande',
        'ob.kicker': 'Du linge frais, à la demande',
        'ob.tagline': 'Réservez un ramassage en quelques minutes — les partenaires viennent à vous.',
        'ob.create': 'Créer un compte',
        'ob.login': 'Se connecter',
        'ob.or': 'ou',
        'ob.forgot': 'Accès oublié ? Réinitialiser par e-mail',
        'ob.emailTitle': 'Créez votre compte',
        'ob.emailCopy': 'Entrez votre e-mail — nous enverrons un code à usage unique. Aucun mot de passe.',
        'ob.email': 'E-mail',
        'ob.continue': 'Continuer',
        'ob.codeTitle': 'Entrez le code',
        'ob.codeCopy': 'Nous avons envoyé un code à 6 chiffres à votre e-mail.',
        'ob.code': 'Code de vérification',
        'ob.resend': 'Renvoyer le code',
        'ob.verify': 'Vérifier',
        'ob.almost': 'Presque terminé',
        'ob.profileCopy': 'Ajoutez votre nom et téléphone pour que les partenaires puissent vous joindre.',
        'ob.name': 'Nom',
        'ob.phone': 'Téléphone',
        'ob.start': 'Commencer',
        'book.title': 'Que lavons-nous aujourd’hui ?',
        'book.sub': 'Trouvez le partenaire de blanchisserie le plus proche, comme une course.',
        'book.now': 'Ramassage maintenant',
        'book.later': 'Planifier plus tard',
        'book.location': 'Lieu de ramassage',
        'book.addrPh': 'Entrez votre adresse de ramassage',
        'book.locSet': '✓ Lieu défini — recherche de partenaires à proximité',
        'book.services': 'Type de service',
        'book.weight': 'Poids estimé',
        'book.payment': 'Mode de paiement',
        'book.promo': 'Code promo',
        'book.apply': 'Appliquer',
        'book.request': 'Définissez d’abord un lieu de ramassage',
        'pay.card': 'Carte bancaire',
        'pay.transfer': 'Virement bancaire',
        'pay.cash': 'Espèces au ramassage',
        'match.title': 'Choisir un partenaire',
        'match.sub': 'Blanchisseries à proximité — choisissez, puis confirmez.',
        'match.eta': 'Heure de ramassage estimée',
        'match.total': 'Total à payer',
        'match.confirm': 'Confirmer le ramassage',
        'match.selectFirst': 'Sélectionnez d’abord un partenaire',
        'match.cancel': 'Annuler',
        'trip.status': 'Statut',
        'trip.eta': 'ETA',
        'trip.assist': 'Assistant skyWash',
        'trip.contactSupport': 'Contacter le support',
        'trip.supportEmail': 'Email support',
        'trip.cancel': 'Annuler la commande',
        'rating.title': 'Livré !',
        'rating.again': 'Réserver un autre ramassage',
        'browse.title': 'Tous les partenaires',
        'browse.sub': 'Parcourez tous les prestataires sur la carte.',
        'browse.search': 'Rechercher par nom, quartier ou ville…',
        'orders.title': 'Mes commandes',
        'orders.sub': 'Vos ramassages passés avec skyWash Cleaning.',
        'acct.title': 'Compte',
        'acct.sub': 'Profil, apparence et langue.',
        'acct.name': 'Nom',
        'acct.phone': 'Téléphone',
        'acct.email': 'E-mail',
        'acct.payment': 'Paiement par défaut',
        'acct.save': 'Enregistrer',
        'acct.saved': 'Profil enregistré.',
        'acct.reset': 'Réinitialiser l’accès (nouveau code)',
        'acct.logout': 'Se déconnecter',
        'acct.logoutAll': 'Se déconnecter partout',
        'prefs.appearance': 'Apparence',
        'prefs.appearanceHint': 'Choisissez clair, sombre, ou suivez l’appareil.',
        'prefs.light': 'Clair',
        'prefs.dark': 'Sombre',
        'prefs.system': 'Système',
        'prefs.language': 'Langue',
        'prefs.languageHint': 'Détectée selon votre pays au premier lancement — vous pouvez la changer ici.',
        'live.default': 'En direct',
        'menu.profile': 'Profil',
        'menu.reset': 'Réinitialiser l’accès',
        'menu.logout': 'Se déconnecter',
        'menu.logoutAll': 'Se déconnecter partout'
      }
    }
  };

  const THEME_KEY = 'skywash_theme';
  const LOCALE_KEY = 'skywash_locale';
  let currentLocale = 'en';

  // Countries where French is a primary / official language
  const FRENCH_COUNTRIES = new Set([
    'FR', 'BE', 'CH', 'LU', 'MC',
    'SN', 'CI', 'ML', 'BF', 'NE', 'TG', 'BJ', 'GN', 'GW',
    'CD', 'CG', 'GA', 'CM', 'TD', 'CF', 'GQ',
    'RW', 'BI', 'DJ', 'KM', 'MG', 'SC', 'MU',
    'HT', 'CA'
  ]);

  function normalizeLangTag(tag) {
    if (!tag) return null;
    const primary = String(tag).toLowerCase().replace('_', '-').split('-')[0];
    return LOCALES[primary] ? primary : null;
  }

  function localeFromBrowser() {
    const list = [];
    try {
      if (navigator.languages) list.push(...navigator.languages);
      if (navigator.language) list.push(navigator.language);
    } catch (_) {}
    for (const tag of list) {
      const code = normalizeLangTag(tag);
      if (code) return code;
    }
    return null;
  }

  function localeFromTimezone() {
    let tz = '';
    try { tz = Intl.DateTimeFormat().resolvedOptions().timeZone || ''; } catch (_) {}
    const map = {
      'Europe/Paris': 'fr',
      'Europe/Brussels': 'fr',
      'Europe/Zurich': 'fr',
      'Europe/Luxembourg': 'fr',
      'Africa/Abidjan': 'fr',
      'Africa/Dakar': 'fr',
      'Africa/Bamako': 'fr',
      'Africa/Ouagadougou': 'fr',
      'Africa/Niamey': 'fr',
      'Africa/Lome': 'fr',
      'Africa/Porto-Novo': 'fr',
      'Africa/Libreville': 'fr',
      'Africa/Douala': 'fr',
      'Africa/Kinshasa': 'fr',
      'Africa/Brazzaville': 'fr',
      'Indian/Antananarivo': 'fr',
      'America/Port-au-Prince': 'fr',
      'Africa/Lagos': 'en',
      'Africa/Accra': 'en',
      'Africa/Nairobi': 'en'
    };
    return map[tz] || null;
  }

  function localeFromCountry(countryCode) {
    if (!countryCode) return null;
    const cc = String(countryCode).toUpperCase();
    if (FRENCH_COUNTRIES.has(cc)) return 'fr';
    if (cc === 'NG') return 'en';
    return null;
  }

  async function detectCountryCode() {
    try {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), 2500);
      const res = await fetch('https://ipapi.co/json/', { signal: ctrl.signal });
      clearTimeout(timer);
      if (!res.ok) return null;
      const data = await res.json();
      return (data.country_code || data.country || '').toUpperCase() || null;
    } catch (_) {
      return null;
    }
  }

  async function resolveInitialLocale() {
    // Prefer an explicit non-English browser language (yo/ha/ig/fr)
    const fromBrowser = localeFromBrowser();
    if (fromBrowser && fromBrowser !== 'en') return fromBrowser;

    const country = await detectCountryCode();
    const fromCountry = localeFromCountry(country);
    if (fromCountry) return fromCountry;

    const fromTz = localeFromTimezone();
    if (fromTz) return fromTz;

    return fromBrowser || 'en';
  }

  function getThemePref() {
    try { return localStorage.getItem(THEME_KEY) || 'system'; } catch (_) { return 'system'; }
  }

  function resolveTheme(pref) {
    const p = pref || getThemePref();
    if (p === 'light' || p === 'dark') return p;
    try {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    } catch (_) {
      return 'light';
    }
  }

  function applyTheme(pref) {
    const choice = pref || getThemePref();
    try { localStorage.setItem(THEME_KEY, choice); } catch (_) {}
    const resolved = resolveTheme(choice);
    document.documentElement.setAttribute('data-theme', resolved);
    document.documentElement.setAttribute('data-theme-pref', choice);
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', resolved === 'dark' ? '#041018' : '#08283a');
    document.querySelectorAll('[data-theme-option]').forEach((btn) => {
      btn.classList.toggle('active', btn.getAttribute('data-theme-option') === choice);
    });
    return choice;
  }

  function getLocale() {
    try {
      const saved = localStorage.getItem(LOCALE_KEY);
      if (saved && LOCALES[saved]) return saved;
    } catch (_) {}
    return currentLocale || 'en';
  }

  function hasSavedLocale() {
    try { return !!localStorage.getItem(LOCALE_KEY); } catch (_) { return false; }
  }

  function t(key, vars) {
    const pack = LOCALES[currentLocale] || LOCALES.en;
    let text = (pack.strings && pack.strings[key]) || (LOCALES.en.strings[key]) || key;
    if (vars) {
      Object.keys(vars).forEach((k) => {
        text = text.replace(new RegExp('\\{' + k + '\\}', 'g'), vars[k]);
      });
    }
    return text;
  }

  function applyI18n(locale) {
    currentLocale = LOCALES[locale] ? locale : 'en';
    try { localStorage.setItem(LOCALE_KEY, currentLocale); } catch (_) {}
    document.documentElement.lang = currentLocale;
    document.querySelectorAll('[data-i18n]').forEach((el) => {
      const key = el.getAttribute('data-i18n');
      if (!key) return;
      el.textContent = t(key);
    });
    document.querySelectorAll('[data-i18n-placeholder]').forEach((el) => {
      const key = el.getAttribute('data-i18n-placeholder');
      if (!key) return;
      el.setAttribute('placeholder', t(key));
    });
    document.querySelectorAll('[data-i18n-aria]').forEach((el) => {
      const key = el.getAttribute('data-i18n-aria');
      if (!key) return;
      el.setAttribute('aria-label', t(key));
    });
    const langSelect = document.getElementById('acctLanguage');
    if (langSelect) langSelect.value = currentLocale;
    return currentLocale;
  }

  function listLocales() {
    return Object.keys(LOCALES).map((code) => ({ code, name: LOCALES[code].name }));
  }

  async function initPrefs() {
    applyTheme(getThemePref());
    let locale = null;
    try { locale = localStorage.getItem(LOCALE_KEY); } catch (_) {}
    if (!locale || !LOCALES[locale]) {
      locale = await resolveInitialLocale();
    }
    applyI18n(locale);
    try {
      window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if (getThemePref() === 'system') applyTheme('system');
      });
    } catch (_) {}
    return locale;
  }

  global.SkywashI18n = {
    t,
    applyTheme,
    applyI18n,
    getThemePref,
    getLocale,
    hasSavedLocale,
    listLocales,
    initPrefs,
    resolveInitialLocale,
    LOCALES
  };
})(window);

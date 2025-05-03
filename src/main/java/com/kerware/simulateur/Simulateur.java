package com.kerware.simulateur;

/**
 * Simulateur d’impôts 2024 basé sur les revenus 2023.
 * Version refactorée avec lisibilité, modularité et traçabilité des exigences.
 */
public class Simulateur {

    // ==== CONSTANTES ====

    // Tranches d'impôt
    private static final int[] TRANCHES = {0, 11294, 28797, 82341, 177106, Integer.MAX_VALUE};
    private static final double[] TAUX = {0.0, 0.11, 0.30, 0.41, 0.45};

    // Tranches et taux de CEHR
    private static final int[] TRANCHES_CEHR = {0, 250000, 500000, 1000000, Integer.MAX_VALUE};
    private static final double[] TAUX_CEHR_CELIB = {0.0, 0.03, 0.04, 0.04};
    private static final double[] TAUX_CEHR_COUPLE = {0.0, 0.0, 0.03, 0.04};

    // Abattement
    private static final double TAUX_ABATTEMENT = 0.10;
    private static final int ABATTEMENT_MIN = 495;
    private static final int ABATTEMENT_MAX = 14171;

    // Plafond baisse d’impôt
    private static final double PLAFOND_DEMI_PART = 1759;

    // Décote
    private static final double SEUIL_DECOTE_SEUL = 1929;
    private static final double SEUIL_DECOTE_COUPLE = 3191;
    private static final double DECOTE_MAX_SEUL = 873;
    private static final double DECOTE_MAX_COUPLE = 1444;
    private static final double TAUX_DECOTE = 0.4525;

    // ==== ATTRIBUTS ====

    private int rNetDecl1, rNetDecl2, nbEnf, nbEnfH;
    private boolean parIso;
    private double rFRef, abt, nbPts, nbPtsDecl;
    private double decote, mImpDecl, mImp, mImpAvantDecote;
    private double contribExceptionnelle;

    // ==== GETTERS POUR TESTS ====

    public double getRevenuReference() { return rFRef; }
    public double getDecote() { return decote; }
    public double getAbattement() { return abt; }
    public double getNbParts() { return nbPts; }
    public double getImpotAvantDecote() { return mImpAvantDecote; }
    public double getImpotNet() { return mImp; }
    public int getRevenuNetDeclarant1() { return rNetDecl1; }
    public int getRevenuNetDeclarant2() { return rNetDecl2; }
    public double getContribExceptionnelle() { return contribExceptionnelle; }

    /**
     * Méthode principale de calcul de l’impôt.
     * @param revNetDecl1 Revenu net du déclarant 1
     * @param revNetDecl2 Revenu net du déclarant 2
     * @param sitFam Situation familiale
     * @param nbEnfants Nombre d'enfants
     * @param nbEnfantsHandicapes Nombre d'enfants handicapés
     * @param parentIsol True si parent isolé
     * @return Impôt net à payer
     */
    public int calculImpot(int revNetDecl1, int revNetDecl2, SituationFamiliale sitFam,
                           int nbEnfants, int nbEnfantsHandicapes, boolean parentIsol) {

        verifierParametres(revNetDecl1, revNetDecl2, sitFam, nbEnfants, nbEnfantsHandicapes, parentIsol);

        // Initialisation
        this.rNetDecl1 = revNetDecl1;
        this.rNetDecl2 = revNetDecl2;
        this.nbEnf = nbEnfants;
        this.nbEnfH = nbEnfantsHandicapes;
        this.parIso = parentIsol;

        // EXIGENCE : EXG_IMPOT_02
        calculAbattement(sitFam);

        rFRef = Math.max(0, rNetDecl1 + rNetDecl2 - abt);

        // EXIGENCE : EXG_IMPOT_03
        calculParts(sitFam);

        // EXIGENCE : EXG_IMPOT_07
        calculContributionExceptionnelle();

        // EXIGENCE : EXG_IMPOT_04
        double impotBrutDecl = calculImpotParTranche(rFRef / nbPtsDecl, nbPtsDecl);
        double impotBrutFoyer = calculImpotParTranche(rFRef / nbPts, nbPts);

        // EXIGENCE : EXG_IMPOT_05
        mImpAvantDecote = appliquerPlafondBaisseImpot(impotBrutDecl, impotBrutFoyer);
        mImp = mImpAvantDecote;

        // EXIGENCE : EXG_IMPOT_06
        appliquerDecote();

        mImp += contribExceptionnelle;

        return (int) Math.round(mImp);
    }

    private void verifierParametres(int rev1, int rev2, SituationFamiliale sf, int enfants, int enfH, boolean iso) {
        if (rev1 < 0 || rev2 < 0) throw new IllegalArgumentException("Revenus négatifs interdits");
        if (sf == null) throw new IllegalArgumentException("Situation familiale manquante");
        if (enfants < 0 || enfants > 7) throw new IllegalArgumentException("Nombre d'enfants invalide");
        if (enfH < 0 || enfH > enfants) throw new IllegalArgumentException("Enfants handicapés incohérents");
        if (iso && (sf == SituationFamiliale.MARIE || sf == SituationFamiliale.PACSE))
            throw new IllegalArgumentException("Parent isolé incompatible avec couple");
        if ((sf == SituationFamiliale.CELIBATAIRE || sf == SituationFamiliale.DIVORCE || sf == SituationFamiliale.VEUF) && rev2 > 0)
            throw new IllegalArgumentException("Revenu déclarant 2 invalide");
    }

    private void calculAbattement(SituationFamiliale sf) {
        long abt1 = Math.max(ABATTEMENT_MIN, Math.min(ABATTEMENT_MAX, Math.round(rNetDecl1 * TAUX_ABATTEMENT)));
        long abt2 = 0;
        if (sf == SituationFamiliale.MARIE || sf == SituationFamiliale.PACSE) {
            abt2 = Math.max(ABATTEMENT_MIN, Math.min(ABATTEMENT_MAX, Math.round(rNetDecl2 * TAUX_ABATTEMENT)));
        }
        abt = abt1 + abt2;
    }

    private void calculParts(SituationFamiliale sf) {
        nbPtsDecl = switch (sf) {
            case MARIE, PACSE -> 2.0;
            default -> 1.0;
        };

        nbPts = nbPtsDecl;
        if (nbEnf <= 2) {
            nbPts += nbEnf * 0.5;
        } else {
            nbPts += 1 + (nbEnf - 2);
        }

        if (parIso && nbEnf > 0) nbPts += 0.5;
        if (sf == SituationFamiliale.VEUF && nbEnf > 0) nbPts += 1;
        nbPts += nbEnfH * 0.5;
    }

    private void calculContributionExceptionnelle() {
        contribExceptionnelle = 0;
        double[] taux = nbPtsDecl == 1 ? TAUX_CEHR_CELIB : TAUX_CEHR_COUPLE;
        for (int i = 0; i < TRANCHES_CEHR.length - 1; i++) {
            if (rFRef > TRANCHES_CEHR[i]) {
                double base = Math.min(rFRef, TRANCHES_CEHR[i + 1]) - TRANCHES_CEHR[i];
                contribExceptionnelle += base * taux[i];
            }
        }
        contribExceptionnelle = Math.round(contribExceptionnelle);
    }

    private double calculImpotParTranche(double revenuParPart, double parts) {
        double impot = 0;
        for (int i = 0; i < TRANCHES.length - 1; i++) {
            if (revenuParPart > TRANCHES[i]) {
                double base = Math.min(revenuParPart, TRANCHES[i + 1]) - TRANCHES[i];
                impot += base * TAUX[i];
            }
        }
        return Math.round(impot * parts);
    }

    private double appliquerPlafondBaisseImpot(double impotDecl, double impotFoyer) {
        double baisse = impotDecl - impotFoyer;
        double ecartParts = nbPts - nbPtsDecl;
        double plafond = (ecartParts / 0.5) * PLAFOND_DEMI_PART;

        return baisse > plafond ? impotDecl - plafond : impotFoyer;
    }

    private void appliquerDecote() {
        decote = 0;
        if (nbPtsDecl == 1 && mImp < SEUIL_DECOTE_SEUL) {
            decote = DECOTE_MAX_SEUL - (mImp * TAUX_DECOTE);
        } else if (nbPtsDecl == 2 && mImp < SEUIL_DECOTE_COUPLE) {
            decote = DECOTE_MAX_COUPLE - (mImp * TAUX_DECOTE);
        }

        decote = Math.round(Math.max(0, Math.min(decote, mImp)));
        mImp -= decote;
    }
}

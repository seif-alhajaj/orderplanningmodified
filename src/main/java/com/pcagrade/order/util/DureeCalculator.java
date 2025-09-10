package com.pcagrade.order.util;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Utility class for standardizing order duration calculation
 *
 * BUSINESS RULE: Duration = 3 × number of cards
 * - Each card requires 3 units of certification time
 * - Minimum duration: 3 minutes (1 card minimum)
 * - Default duration if missing data: 60 minutes
 */
@Component
public class DureeCalculator {

    /**
     * Constant: certification time per card (in minutes)
     */
    public static final int TEMPS_CERTIFICATION_PAR_CARTE = 3;

    /**
     * Default duration if unable to calculate (in minutes)
     */
    public static final int DUREE_DEFAUT_MINUTES = 60;

    /**
     * Minimum order duration (in minutes)
     */
    public static final int DUREE_MINIMALE_MINUTES = 3; // 1 card minimum

    /**
     * Calculates order duration based on number of cards
     *
     * @param nombreCartes Number of cards in the order
     * @return Duration in minutes (3 × nombreCartes)
     */
    public static int calculerDureeMinutes(int nombreCartes) {
        if (nombreCartes <= 0) {
            return DUREE_MINIMALE_MINUTES; // 1 card minimum
        }
        return nombreCartes * TEMPS_CERTIFICATION_PAR_CARTE;
    }

    /**
     * Calculates order duration from database data
     *
     * @param commandeData Map containing order data
     * @return Duration in minutes calculated according to business rules
     */
    public static int calculerDureeDepuisCommande(Map<String, Object> commandeData) {
        try {
            // 1. Priority to real number of cards
            Integer nombreCartesReelles = (Integer) commandeData.get("nombreCartesReelles");
            if (nombreCartesReelles != null && nombreCartesReelles > 0) {
                return calculerDureeMinutes(nombreCartesReelles);
            }

            // 2. Fallback to nombreCartes
            Integer nombreCartes = (Integer) commandeData.get("nombreCartes");
            if (nombreCartes != null && nombreCartes > 0) {
                return calculerDureeMinutes(nombreCartes);
            }

            // 3. Fallback to existing dureeMinutes if consistent
            Integer dureeExistante = (Integer) commandeData.get("dureeMinutes");
            if (dureeExistante != null && dureeExistante >= DUREE_MINIMALE_MINUTES) {
                // Check if this duration is consistent with the card rule
                int cartesImpliquees = dureeExistante / TEMPS_CERTIFICATION_PAR_CARTE;
                if (dureeExistante == cartesImpliquees * TEMPS_CERTIFICATION_PAR_CARTE) {
                    return dureeExistante; // Consistent duration
                }
            }

            // 4. Last resort: default duration
            System.out.println("No card data for order " +
                    commandeData.get("numeroCommande") + ", using default duration");
            return DUREE_DEFAUT_MINUTES;

        } catch (Exception e) {
            System.err.println("Duration calculation error for order " +
                    commandeData.get("numeroCommande") + ": " + e.getMessage());
            return DUREE_DEFAUT_MINUTES;
        }
    }

    /**
     * Converts duration in minutes to a readable format
     *
     * @param dureeMinutes Duration in minutes
     * @return Format "Xh Ymin" or "Ymin"
     */
    public static String formaterDuree(int dureeMinutes) {
        if (dureeMinutes < 60) {
            return dureeMinutes + "min";
        }

        int heures = dureeMinutes / 60;
        int minutes = dureeMinutes % 60;

        if (minutes == 0) {
            return heures + "h";
        } else {
            return heures + "h " + minutes + "min";
        }
    }

    /**
     * Calculates theoretical number of cards based on duration
     * Useful for reverse validation
     *
     * @param dureeMinutes Duration in minutes
     * @return Theoretical number of cards
     */
    public static int calculerNombreCartesTheorique(int dureeMinutes) {
        return Math.max(1, dureeMinutes / TEMPS_CERTIFICATION_PAR_CARTE);
    }

    /**
     * Validates consistency between number of cards and duration
     *
     * @param nombreCartes Declared number of cards
     * @param dureeMinutes Declared duration
     * @return true if consistent, false otherwise
     */
    public static boolean validerCoherence(int nombreCartes, int dureeMinutes) {
        int dureeAttendue = calculerDureeMinutes(nombreCartes);
        return dureeAttendue == dureeMinutes;
    }

    /**
     * Generates a duration calculation report for debugging
     *
     * @param commandeData Order data
     * @return Detailed calculation report
     */
    public static String genererRapportCalcul(Map<String, Object> commandeData) {
        StringBuilder rapport = new StringBuilder();
        rapport.append("Duration calculation for order ").append(commandeData.get("numeroCommande")).append(":\n");

        Integer nombreCartesReelles = (Integer) commandeData.get("nombreCartesReelles");
        Integer nombreCartes = (Integer) commandeData.get("nombreCartes");
        Integer dureeExistante = (Integer) commandeData.get("dureeMinutes");

        rapport.append("- Real cards: ").append(nombreCartesReelles).append("\n");
        rapport.append("- Declared cards: ").append(nombreCartes).append("\n");
        rapport.append("- Existing duration: ").append(dureeExistante).append(" min\n");

        int dureeCalculee = calculerDureeDepuisCommande(commandeData);
        rapport.append("- Calculated duration: ").append(dureeCalculee).append(" min (");
        rapport.append(formaterDuree(dureeCalculee)).append(")\n");
        rapport.append("- Cards involved: ").append(calculerNombreCartesTheorique(dureeCalculee));

        return rapport.toString();
    }
}
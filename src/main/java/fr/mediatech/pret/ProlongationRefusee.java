package fr.mediatech.pret;

/**
 * Levée quand une prolongation est refusée.
 * Le message explique la raison, dans le langage du métier.
 */
public class ProlongationRefusee extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProlongationRefusee(String raison) {
        super(raison);
    }
}

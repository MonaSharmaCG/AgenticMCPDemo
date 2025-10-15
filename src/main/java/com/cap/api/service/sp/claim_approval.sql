
CREATE OR REPLACE PROCEDURE ClaimApprovalProcedure (
    p_claim_id       IN claim.claim_id%TYPE,
    p_approved_by    IN VARCHAR2  -- assuming the name or ID of the person who approves the claim
) AS
    v_claim_status claim.approval_status%TYPE;
    v_claim_amount claim.claim_amount%TYPE;
    v_claim_date claim.claim_date%TYPE;
    v_threshold NUMBER := 10000; -- Setting a threshold amount
BEGIN
    -- Retrieve the current status, amount, and date of the claim
    SELECT approval_status, claim_amount, claim_date INTO v_claim_status, v_claim_amount, v_claim_date
    FROM claim
    WHERE claim_id = p_claim_id;

    -- Check if the claim is already approved or rejected
    IF v_claim_status IS NOT NULL THEN
        DBMS_OUTPUT.PUT_LINE('Claim ' || p_claim_id || ' has already been ' || v_claim_status || '.');
    -- Check if the claim amount exceeds the threshold

    -- Check if the claim date is valid
    ELSIF v_claim_date > SYSDATE OR v_claim_date < ADD_MONTHS(SYSDATE, -6) THEN
        DBMS_OUTPUT.PUT_LINE('Claim ' || p_claim_id || ' has an invalid claim date.');
    -- Check if the claim is valid and can be approved
    ELSIF v_claim_status IS NULL THEN
        -- Your approval logic here
        UPDATE claim
        SET approval_status = 'Approved'
        WHERE claim_id = p_claim_id;



        DBMS_OUTPUT.PUT_LINE('Claim ' || p_claim_id || ' approved successfully by ' || p_approved_by || '.');
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        DBMS_OUTPUT.PUT_LINE('Error: Claim ID ' || p_claim_id || ' not found.');
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Error: Unable to approve claim.');
END;
/
